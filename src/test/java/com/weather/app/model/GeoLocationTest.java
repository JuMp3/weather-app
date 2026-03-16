package com.weather.app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoLocation.getDisplayName()")
class GeoLocationTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private GeoLocation build(String name, String region, String country) {
        GeoLocation loc = new GeoLocation();
        loc.setName(name);
        loc.setRegion(region);
        loc.setCountry(country);
        return loc;
    }

    // -------------------------------------------------------------------------
    // Only name provided
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("region and country are both absent")
    class NeitherRegionNorCountryTest {

        @Test
        @DisplayName("region null, country null → only name")
        void bothNull() {
            GeoLocation loc = build("Roma", null, null);
            assertThat(loc.getDisplayName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("region blank, country null → only name")
        void regionBlankCountryNull() {
            GeoLocation loc = build("Roma", "  ", null);
            assertThat(loc.getDisplayName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("region null, country blank → only name")
        void regionNullCountryBlank() {
            GeoLocation loc = build("Roma", null, "   ");
            assertThat(loc.getDisplayName()).isEqualTo("Roma");
        }

        @Test
        @DisplayName("region blank, country blank → only name")
        void bothBlank() {
            GeoLocation loc = build("Roma", "", "");
            assertThat(loc.getDisplayName()).isEqualTo("Roma");
        }
    }

    // -------------------------------------------------------------------------
    // Region present, country absent
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("region present, country absent")
    class RegionPresentCountryAbsentTest {

        @Test
        @DisplayName("region set, country null → name + region")
        void regionSetCountryNull() {
            GeoLocation loc = build("Roma", "Lazio", null);
            assertThat(loc.getDisplayName()).isEqualTo("Roma, Lazio");
        }

        @Test
        @DisplayName("region set, country blank → name + region")
        void regionSetCountryBlank() {
            GeoLocation loc = build("Roma", "Lazio", "");
            assertThat(loc.getDisplayName()).isEqualTo("Roma, Lazio");
        }

        @Test
        @DisplayName("region set with spaces, country blank → name + region (spaces preserved)")
        void regionWithSpacesCountryBlank() {
            GeoLocation loc = build("Roma", "  Lazio  ", "");
            // isBlank() returns false for "  Lazio  " → still appended as-is
            assertThat(loc.getDisplayName()).isEqualTo("Roma,   Lazio  ");
        }
    }

    // -------------------------------------------------------------------------
    // Region absent, country present
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("region absent, country present")
    class RegionAbsentCountryPresentTest {

        @Test
        @DisplayName("region null, country set → name + country")
        void regionNullCountrySet() {
            GeoLocation loc = build("Roma", null, "Italy");
            assertThat(loc.getDisplayName()).isEqualTo("Roma, Italy");
        }

        @Test
        @DisplayName("region blank, country set → name + country")
        void regionBlankCountrySet() {
            GeoLocation loc = build("Roma", "", "Italy");
            assertThat(loc.getDisplayName()).isEqualTo("Roma, Italy");
        }

        @Test
        @DisplayName("region whitespace-only, country set → name + country")
        void regionWhitespaceCountrySet() {
            GeoLocation loc = build("Roma", "   ", "Italy");
            assertThat(loc.getDisplayName()).isEqualTo("Roma, Italy");
        }
    }

    // -------------------------------------------------------------------------
    // Both region and country present
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("region and country both present")
    class BothPresentTest {

        @Test
        @DisplayName("all three set → name + region + country")
        void allSet() {
            GeoLocation loc = build("Roma", "Lazio", "Italy");
            assertThat(loc.getDisplayName()).isEqualTo("Roma, Lazio, Italy");
        }

        @Test
        @DisplayName("empty name → still produces separators")
        void emptyName() {
            GeoLocation loc = build("", "Lazio", "Italy");
            assertThat(loc.getDisplayName()).isEqualTo(", Lazio, Italy");
        }
    }
}
