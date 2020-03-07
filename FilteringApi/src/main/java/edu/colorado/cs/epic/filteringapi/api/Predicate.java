package edu.colorado.cs.epic.filteringapi.api;

import edu.colorado.cs.epic.filteringapi.api.Expression;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class Predicate {
    @NotNull
    Boolean isOr;

    @NotEmpty
    @Valid
    Expression expressions[];

    public Predicate() {
    }

    @JsonProperty
    public void setIsOr(@NotNull Boolean isOr) {
        this.isOr = isOr;
    }

    @JsonProperty
    public Boolean getIsOr() {
        return isOr;
    }

    @JsonProperty
    public void setExpressions(Expression expressions[]) {
        this.expressions = expressions;
    }

    @JsonProperty
    public Expression[] getExpressions() {
        return expressions;
    }

    public String paramString() {
        String paramString = "";

        for (int i = 0; i < expressions.length; i++) {
            Expression expression = expressions[i];
            if (i > 0) {
                paramString += expression.isOr ? "OR" : "AND";
            }
            paramString += expression.getSelectValue();
            paramString += expression.getText();
        }

        return paramString;
    }

    public String buildQueryPredicate() {
        String queryExpression = "(";

        for (int i = 0; i < expressions.length; i++) {
            Expression expression = expressions[i];

            if (i > 0) {
                queryExpression += expression.isOr ? ") OR (" : ") AND (";
            }

            switch (expression.selectValue) {
            case ALLWORDS:
                queryExpression += buildTweetTextCondition(expression.getTextArr(), false, false);
                break;
            case ANYWORDS:
                queryExpression += buildTweetTextCondition(expression.getTextArr(), false, true);
                break;
            case NOTWORDS:
                queryExpression += buildTweetTextCondition(expression.getTextArr(), true, false);
                break;
            case PHRASE:
                queryExpression += buildTweetTextCondition(expression.getTextArr(), false, false);
                break;
            default:
                break;
            }
        }
        queryExpression += ")";
        return queryExpression;
    }

    private String buildTweetTextCondition(String[] arr, Boolean isNot, Boolean isOr) {
        String clause = "";
        String extendedText = "";
        String condition = isOr ? " OR " : " AND ";
        String notStr = isNot ? "NOT LIKE" : "LIKE";
        for (int i = 0; i < arr.length; i++) {
            clause += String.format("LOWER(text) %s '%%%s%%'", notStr, arr[i]);
            extendedText += String.format("LOWER(extended_tweet.full_text) %s '%%%s%%'", notStr, arr[i]);
            if (i < arr.length - 1) {
                clause += condition;
                extendedText += condition;
            }
        }
        clause += String.format(" OR %s", extendedText);
        return clause;
    }
}