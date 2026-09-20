param(
    [Parameter(Mandatory = $true)][Guid]$EventId,
    [string]$ComposeFile = 'docker-compose.yaml',
    [string]$EnvFile
)
$ErrorActionPreference = 'Stop'
$composeArgs = @('compose')
if ($EnvFile) { $composeArgs += @('--env-file', $EnvFile) }
$composeArgs += @('-f', $ComposeFile)
$statement = @"
UPDATE message_outbox
SET state='PENDING', failures=0, publish_attempts=0, publish_token=NULL,
    last_error=NULL, next_attempt_at=CURRENT_TIMESTAMP(6), updated_at=CURRENT_TIMESTAMP(6)
WHERE event_id='$($EventId.ToString())' AND state='DEAD';
SELECT ROW_COUNT() AS replayed_events;
"@
$statement | & docker @composeArgs exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -N -B picture_management'
if ($LASTEXITCODE -ne 0) { throw 'Message replay failed.' }
# The dead-letter copy is retained as evidence. Purge only after all dead events are resolved.
