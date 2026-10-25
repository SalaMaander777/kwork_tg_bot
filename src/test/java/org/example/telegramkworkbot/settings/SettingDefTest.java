package org.example.telegramkworkbot.settings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettingDefTest {

    private static SettingDef def(SettingDef.Type type, Double min, Double max, List<String> options) {
        return new SettingDef("k", type, "g", "label", null, "1", min, max, options);
    }

    @Test
    void validatesNumbersAndRanges() {
        SettingDef interval = def(SettingDef.Type.INTEGER, 10.0, 3600.0, List.of());
        assertThat(interval.validate("60")).isNull();
        assertThat(interval.validate("5")).isEqualTo("Не меньше 10");
        assertThat(interval.validate("4000")).isEqualTo("Не больше 3600");
        assertThat(interval.validate("1.5")).isEqualTo("Ожидается число");

        SettingDef temperature = def(SettingDef.Type.DECIMAL, 0.0, 2.0, List.of());
        assertThat(temperature.validate("0.7")).isNull();
        assertThat(temperature.validate("2.5")).isEqualTo("Не больше 2");
    }

    @Test
    void validatesBooleanChoiceAndText() {
        assertThat(def(SettingDef.Type.BOOLEAN, null, null, List.of()).validate("yes")).isNotNull();
        assertThat(def(SettingDef.Type.BOOLEAN, null, null, List.of()).validate("true")).isNull();
        SettingDef model = def(SettingDef.Type.CHOICE, null, null, List.of("GigaChat", "GigaChat-2-Max"));
        assertThat(model.validate("GigaChat-2-Max")).isNull();
        assertThat(model.validate("gpt-4")).startsWith("Допустимые значения");
        SettingDef text = def(SettingDef.Type.TEXT, null, 10.0, List.of());
        assertThat(text.validate(" ")).isNotNull();
        assertThat(text.validate("a".repeat(11))).isEqualTo("Не длиннее 10 символов");
    }
}
