package com.picmgmt.service.oauth;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthUsernameGeneratorTest {

    @Test
    void publicNicknameTakesPriorityOverEmailLikeProviderUsername() {
        assertEquals("A", OAuthUsernameGenerator.createBaseUsername(
                "google", "A", "123@gmail.com", "123@gmail.com", "google-subject"
        ));
    }

    @Test
    void providerUsernameAndEmailLocalPartRemainUsefulFallbacks() {
        assertEquals("octocat", OAuthUsernameGenerator.createBaseUsername(
                "github", null, "octocat", null, "github-subject"
        ));
        assertEquals("picture_owner", OAuthUsernameGenerator.createBaseUsername(
                "google", null, "picture.owner@gmail.com", "picture.owner@gmail.com", "google-subject"
        ));
    }

    @Test
    void stableProviderIdFallbackDoesNotExposeTheEmail() {
        String first = OAuthUsernameGenerator.createBaseUsername(
                "google", null, null, null, "stable-google-subject"
        );
        String second = OAuthUsernameGenerator.createBaseUsername(
                "google", null, null, null, "stable-google-subject"
        );

        assertEquals(first, second);
        assertTrue(first.startsWith("google_"));
    }

    @Test
    void duplicateNamesUseHumanReadableSuffixesWithinTheDatabaseLimit() {
        Set<String> existing = Set.of("Astral_Creator", "Astral_Creator_2");

        assertEquals("Astral_Creator_3", OAuthUsernameGenerator.ensureUnique(
                "Astral_Creator", existing::contains
        ));
        assertTrue(OAuthUsernameGenerator.ensureUnique("x".repeat(50), value -> value.equals("x".repeat(50)))
                .length() <= 50);
    }
}
