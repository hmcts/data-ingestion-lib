package uk.gov.hmcts.reform.data.ingestion.camel.validator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.valueOf;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringUtils.isEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DatePatternValidator implements ConstraintValidator<DatePattern, String> {

    String isNullAllowed;

    String pattern;

    public void initialize(DatePattern constraint) {
        isNullAllowed = constraint.isNullAllowed();
        pattern = constraint.regex();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (valueOf(isNullAllowed).equals(TRUE) && isEmpty(value)) {
            return true;
        } else {
            return isEmpty(value) ? FALSE : compile(pattern).matcher(value).matches();
        }
    }
}


