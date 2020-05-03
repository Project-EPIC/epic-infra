package edu.colorado.cs.epic.filteringapi.api;

import edu.colorado.cs.epic.filteringapi.api.ExpressionSelect;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class Expression {
    @NotNull
    Boolean isOr;

    @NotNull
    ExpressionSelect selectValue;

    @NotNull
    String text;

    @NotNull
    String[] textArr;

    public Expression() {
    }

    public Expression(Boolean isOr, ExpressionSelect selectValue, String text) {
        this.isOr = isOr;
        this.selectValue = selectValue;
        this.text = text;
    }

    @JsonProperty
    public void setIsOr(Boolean isOr) {
        this.isOr = isOr;
    }

    @JsonProperty
    public Boolean getIsOr() {
        return isOr;
    }

    @JsonProperty
    public void setSelectValue(ExpressionSelect selectValue) {
        this.selectValue = selectValue;
    }

    @JsonProperty
    public ExpressionSelect getSelectValue() {
        return selectValue;
    }

    @JsonProperty
    public void setText(String text) {
        String[] textArr = text.toLowerCase().split(",");
        Arrays.sort(textArr);

        this.text = String.join(",", textArr);
        this.textArr = textArr;
    }

    @JsonProperty
    public String getText() {
        return text;
    }

    public String[] getTextArr() {
        return textArr;
    }
}