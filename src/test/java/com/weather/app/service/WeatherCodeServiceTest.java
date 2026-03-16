package com.weather.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherCodeService")
class WeatherCodeServiceTest {

    private WeatherCodeService service;

    @BeforeEach
    void setUp() {
        service = new WeatherCodeService();
    }

    // -------------------------------------------------------------------------
    // getDescription
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getDescription")
    class GetDescriptionTest {

        @Test
        @DisplayName("code 0 → Cielo sereno")
        void code0() {
            assertThat(service.getDescription(0)).isEqualTo("Cielo sereno");
        }

        @Test
        @DisplayName("code 1 → Prevalentemente sereno")
        void code1() {
            assertThat(service.getDescription(1)).isEqualTo("Prevalentemente sereno");
        }

        @Test
        @DisplayName("code 2 → Parzialmente nuvoloso")
        void code2() {
            assertThat(service.getDescription(2)).isEqualTo("Parzialmente nuvoloso");
        }

        @Test
        @DisplayName("code 3 → Coperto")
        void code3() {
            assertThat(service.getDescription(3)).isEqualTo("Coperto");
        }

        @Test
        @DisplayName("code 45 → Nebbia")
        void code45() {
            assertThat(service.getDescription(45)).isEqualTo("Nebbia");
        }

        @Test
        @DisplayName("code 48 → Nebbia con brina")
        void code48() {
            assertThat(service.getDescription(48)).isEqualTo("Nebbia con brina");
        }

        @ParameterizedTest(name = "code {0} → drizzle variant")
        @CsvSource({
            "51, Pioggerella leggera",
            "53, Pioggerella moderata",
            "55, Pioggerella intensa",
            "56, Pioggerella gelata leggera",
            "57, Pioggerella gelata intensa"
        })
        void drizzleCodes(int code, String expected) {
            assertThat(service.getDescription(code)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "code {0} → rain variant")
        @CsvSource({
            "61, Pioggia leggera",
            "63, Pioggia moderata",
            "65, Pioggia intensa",
            "66, Pioggia gelata leggera",
            "67, Pioggia gelata intensa"
        })
        void rainCodes(int code, String expected) {
            assertThat(service.getDescription(code)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "code {0} → snow variant")
        @CsvSource({
            "71, Neve leggera",
            "73, Neve moderata",
            "75, Neve intensa",
            "77, Granelli di neve"
        })
        void snowCodes(int code, String expected) {
            assertThat(service.getDescription(code)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "code {0} → shower variant")
        @CsvSource({
            "80, Rovesci leggeri",
            "81, Rovesci moderati",
            "82, Rovesci violenti",
            "85, Rovesci di neve leggeri",
            "86, Rovesci di neve intensi"
        })
        void showerCodes(int code, String expected) {
            assertThat(service.getDescription(code)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "code {0} → thunderstorm variant")
        @CsvSource({
            "95, Temporale",
            "96, Temporale con grandine leggera",
            "99, Temporale con grandine intensa"
        })
        void thunderstormCodes(int code, String expected) {
            assertThat(service.getDescription(code)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "unknown code {0} → Condizioni sconosciute")
        @ValueSource(ints = {-1, 4, 10, 100, 999})
        void unknownCodes(int code) {
            assertThat(service.getDescription(code)).isEqualTo("Condizioni sconosciute");
        }
    }

    // -------------------------------------------------------------------------
    // getIcon
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getIcon")
    class GetIconTest {

        @Test
        @DisplayName("code 0, day → sun emoji")
        void code0Day() {
            assertThat(service.getIcon(0, true)).isEqualTo("☀️");
        }

        @Test
        @DisplayName("code 0, night → moon emoji")
        void code0Night() {
            assertThat(service.getIcon(0, false)).isEqualTo("🌙");
        }

        @Test
        @DisplayName("code 1, day → partly sunny emoji")
        void code1Day() {
            assertThat(service.getIcon(1, true)).isEqualTo("🌤️");
        }

        @Test
        @DisplayName("code 1, night → moon emoji")
        void code1Night() {
            assertThat(service.getIcon(1, false)).isEqualTo("🌙");
        }

        @Test
        @DisplayName("code 2 → cloudy emoji (day/night same)")
        void code2() {
            assertThat(service.getIcon(2, true)).isEqualTo("⛅");
            assertThat(service.getIcon(2, false)).isEqualTo("⛅");
        }

        @Test
        @DisplayName("code 3 → overcast emoji")
        void code3() {
            assertThat(service.getIcon(3, true)).isEqualTo("☁️");
            assertThat(service.getIcon(3, false)).isEqualTo("☁️");
        }

        @ParameterizedTest(name = "fog code {0}")
        @ValueSource(ints = {45, 48})
        void fogCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("🌫️");
            assertThat(service.getIcon(code, false)).isEqualTo("🌫️");
        }

        @ParameterizedTest(name = "drizzle code {0}")
        @ValueSource(ints = {51, 53, 55, 56, 57})
        void drizzleCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("🌦️");
            assertThat(service.getIcon(code, false)).isEqualTo("🌦️");
        }

        @ParameterizedTest(name = "rain code {0}")
        @ValueSource(ints = {61, 63, 65, 66, 67})
        void rainCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("🌧️");
            assertThat(service.getIcon(code, false)).isEqualTo("🌧️");
        }

        @ParameterizedTest(name = "snow code {0}")
        @ValueSource(ints = {71, 73, 75, 77})
        void snowCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("❄️");
            assertThat(service.getIcon(code, false)).isEqualTo("❄️");
        }

        @ParameterizedTest(name = "rain shower code {0}")
        @ValueSource(ints = {80, 81, 82})
        void rainShowerCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("🌩️");
            assertThat(service.getIcon(code, false)).isEqualTo("🌩️");
        }

        @ParameterizedTest(name = "snow shower code {0}")
        @ValueSource(ints = {85, 86})
        void snowShowerCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("🌨️");
            assertThat(service.getIcon(code, false)).isEqualTo("🌨️");
        }

        @ParameterizedTest(name = "thunderstorm code {0}")
        @ValueSource(ints = {95, 96, 99})
        void thunderstormCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("⛈️");
            assertThat(service.getIcon(code, false)).isEqualTo("⛈️");
        }

        @ParameterizedTest(name = "unknown code {0} → thermometer emoji")
        @ValueSource(ints = {-1, 4, 100, 999})
        void unknownCodes(int code) {
            assertThat(service.getIcon(code, true)).isEqualTo("🌡️");
            assertThat(service.getIcon(code, false)).isEqualTo("🌡️");
        }
    }

    // -------------------------------------------------------------------------
    // getBackgroundClass
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getBackgroundClass")
    class GetBackgroundClassTest {

        @Test
        @DisplayName("night (!isDay) always returns bg-night regardless of code")
        void nightAlwaysBgNight() {
            for (int code : new int[]{0, 1, 2, 3, 45, 48, 51, 61, 71, 80, 85, 95, 999}) {
                assertThat(service.getBackgroundClass(code, false))
                        .as("code %d at night", code)
                        .isEqualTo("bg-night");
            }
        }

        @ParameterizedTest(name = "day code {0} → bg-sunny")
        @ValueSource(ints = {0, 1})
        void sunnyCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-sunny");
        }

        @ParameterizedTest(name = "day code {0} → bg-cloudy")
        @ValueSource(ints = {2, 3})
        void cloudyCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-cloudy");
        }

        @ParameterizedTest(name = "day code {0} → bg-foggy")
        @ValueSource(ints = {45, 48})
        void foggyCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-foggy");
        }

        @ParameterizedTest(name = "day code {0} → bg-rainy")
        @ValueSource(ints = {51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82})
        void rainyCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-rainy");
        }

        @ParameterizedTest(name = "day code {0} → bg-snowy")
        @ValueSource(ints = {71, 73, 75, 77, 85, 86})
        void snowyCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-snowy");
        }

        @ParameterizedTest(name = "day code {0} → bg-stormy")
        @ValueSource(ints = {95, 96, 99})
        void stormyCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-stormy");
        }

        @ParameterizedTest(name = "day unknown code {0} → bg-default")
        @ValueSource(ints = {-1, 4, 10, 100, 999})
        void defaultCodes(int code) {
            assertThat(service.getBackgroundClass(code, true)).isEqualTo("bg-default");
        }
    }

    // -------------------------------------------------------------------------
    // getWindDirection
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getWindDirection")
    class GetWindDirectionTest {

        @Test
        @DisplayName("0° → N")
        void north() {
            assertThat(service.getWindDirection(0)).isEqualTo("N");
        }

        @Test
        @DisplayName("90° → E")
        void east() {
            assertThat(service.getWindDirection(90)).isEqualTo("E");
        }

        @Test
        @DisplayName("180° → S")
        void south() {
            assertThat(service.getWindDirection(180)).isEqualTo("S");
        }

        @Test
        @DisplayName("270° → O")
        void west() {
            assertThat(service.getWindDirection(270)).isEqualTo("O");
        }

        @Test
        @DisplayName("348° → NNO")
        void northNorthWest() {
            assertThat(service.getWindDirection(348)).isEqualTo("NNO");
        }

        @Test
        @DisplayName("360° → N (wraps around)")
        void northFull() {
            assertThat(service.getWindDirection(360)).isEqualTo("N");
        }

        @Test
        @DisplayName("45° → NE")
        void northEast() {
            assertThat(service.getWindDirection(45)).isEqualTo("NE");
        }

        @Test
        @DisplayName("135° → SE")
        void southEast() {
            assertThat(service.getWindDirection(135)).isEqualTo("SE");
        }

        @Test
        @DisplayName("225° → SO")
        void southWest() {
            assertThat(service.getWindDirection(225)).isEqualTo("SO");
        }

        @Test
        @DisplayName("315° → NO")
        void northWest() {
            assertThat(service.getWindDirection(315)).isEqualTo("NO");
        }
    }
}
