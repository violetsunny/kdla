package top.kdla.framework.validator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DBLengthValid.Validator.class)
public @interface DBLengthValid {

    String message() default "{value length is invalid}";

    /**
     * 字段长度
     * @return
     */
    int length() default 1;

    /**
     * 小数点后的长度
     * @return
     */
    int decimal() default 2;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<DBLengthValid, Object> {

        private int length;

        private int decimal;

        @Override
        public void initialize(DBLengthValid dbLength) {
            this.length = dbLength.length();
            this.decimal = dbLength.decimal();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
            if (value == null) {
                return Boolean.TRUE;
            }

            try {
                if (value instanceof String) {
                    return ((String)value).length() <= this.length;
                }
                if (value instanceof Integer) {
                    return (Integer)value <= new BigDecimal(10).pow(this.length).intValue() - 1;
                }
                if (value instanceof Long) {
                    return (Long)value <= new BigDecimal(10).pow(this.length).longValue() - 1;
                }
                if (value instanceof BigDecimal) {
                    BigDecimal num = new BigDecimal(10).pow(this.length).subtract(new BigDecimal(1));
                    BigDecimal num2 = new BigDecimal(10).pow(this.decimal).subtract(new BigDecimal(1))
                        .divide(new BigDecimal(10).pow(this.decimal));
                    return ((BigDecimal)value).compareTo(num.add(num2)) <= 0;
                }
                return Boolean.FALSE;
            } catch (Exception e) {
                e.printStackTrace();
                return Boolean.FALSE;
            }

        }

    }
}
