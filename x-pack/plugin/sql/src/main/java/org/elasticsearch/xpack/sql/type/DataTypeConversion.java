/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.type;

import org.elasticsearch.common.Booleans;
import org.elasticsearch.xpack.sql.SqlIllegalArgumentException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Locale;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.LongFunction;

import static org.elasticsearch.xpack.sql.type.DataType.BOOLEAN;
import static org.elasticsearch.xpack.sql.type.DataType.DATE;
import static org.elasticsearch.xpack.sql.type.DataType.LONG;
import static org.elasticsearch.xpack.sql.type.DataType.NULL;

/**
 * Conversions from one Elasticsearch data type to another Elasticsearch data types.
 * <p>
 * This class throws {@link SqlIllegalArgumentException} to differentiate between validation
 * errors inside SQL as oppose to the rest of ES.
 */
public abstract class DataTypeConversion {

    private static final DateTimeFormatter UTC_DATE_FORMATTER = ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC();

    /**
     * Returns the type compatible with both left and right types
     * <p>
     * If one of the types is null - returns another type
     * If both types are numeric - returns type with the highest precision int &lt; long &lt; float &lt; double
     * If one of the types is string and another numeric - returns numeric
     */
    public static DataType commonType(DataType left, DataType right) {
        if (left == right) {
            return left;
        }
        if (DataTypes.isNull(left)) {
            return right;
        }
        if (DataTypes.isNull(right)) {
            return left;
        }
        if (left.isNumeric() && right.isNumeric()) {
            // if one is int
            if (left.isInteger) {
                // promote the highest int
                if (right.isInteger) {
                    return left.size > right.size ? left : right;
                }
                // promote the rational
                return right;
            }
            // try the other side
            if (right.isInteger) {
                return left;
            }
            // promote the highest rational
            return left.size > right.size ? left : right;
        }
        if (left.isString()) {
            if (right.isNumeric()) {
                return right;
            }
        }
        if (right.isString()) {
            if (left.isNumeric()) {
                return left;
            }
        }
        // none found
        return null;
    }

    /**
     * Returns true if the from type can be converted to the to type, false - otherwise
     */
    public static boolean canConvert(DataType from, DataType to) {
        // Special handling for nulls and if conversion is not requires
        if (from == to || from == NULL) {
            return true;
        }
        // only primitives are supported so far
        return from.isPrimitive() && to.isPrimitive() && conversion(from, to) != null;
    }

    /**
     * Get the conversion from one type to another.
     */
    public static Conversion conversionFor(DataType from, DataType to) {
        // Special handling for nulls and if conversion is not requires
        if (from == to) {
            return Conversion.IDENTITY;
        }
        if (to == DataType.NULL) {
            return Conversion.NULL;
        }
        
        Conversion conversion = conversion(from, to);
        if (conversion == null) {
            throw new SqlIllegalArgumentException("cannot convert from [" + from + "] to [" + to + "]");
        }
        return conversion;
    }

    private static Conversion conversion(DataType from, DataType to) {
        switch (to) {
            case KEYWORD:
            case TEXT:
                return conversionToString(from);
            case LONG:
                return conversionToLong(from);
            case INTEGER:
                return conversionToInt(from);
            case SHORT:
                return conversionToShort(from);
            case BYTE:
                return conversionToByte(from);
            case FLOAT:
                return conversionToFloat(from);
            case DOUBLE:
                return conversionToDouble(from);
            case DATE:
                return conversionToDate(from);
            case BOOLEAN:
                return conversionToBoolean(from);
            default:
                return null;
        }

    }

    private static Conversion conversionToString(DataType from) {
        if (from == DATE) {
            return Conversion.DATE_TO_STRING;
        }
        return Conversion.OTHER_TO_STRING;
    }

    private static Conversion conversionToLong(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_LONG;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_LONG;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_INT; // We emit an int here which is ok because of Java's casting rules
        }
        if (from.isString()) {
            return Conversion.STRING_TO_LONG;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_LONG;
        }
        return null;
    }

    private static Conversion conversionToInt(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_INT;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_INT;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_INT;
        }
        if (from.isString()) {
            return Conversion.STRING_TO_INT;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_INT;
        }
        return null;
    }

    private static Conversion conversionToShort(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_SHORT;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_SHORT;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_SHORT;
        }
        if (from.isString()) {
            return Conversion.STRING_TO_SHORT;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_SHORT;
        }
        return null;
    }

    private static Conversion conversionToByte(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_BYTE;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_BYTE;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_BYTE;
        }
        if (from.isString()) {
            return Conversion.STRING_TO_BYTE;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_BYTE;
        }
        return null;
    }

    private static Conversion conversionToFloat(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_FLOAT;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_FLOAT;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_FLOAT;
        }
        if (from.isString()) {
            return Conversion.STRING_TO_FLOAT;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_FLOAT;
        }
        return null;
    }

    private static Conversion conversionToDouble(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_DOUBLE;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_DOUBLE;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_DOUBLE;
        }
        if (from.isString()) {
            return Conversion.STRING_TO_DOUBLE;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_DOUBLE;
        }
        return null;
    }

    private static Conversion conversionToDate(DataType from) {
        if (from.isRational) {
            return Conversion.RATIONAL_TO_DATE;
        }
        if (from.isInteger) {
            return Conversion.INTEGER_TO_DATE;
        }
        if (from == BOOLEAN) {
            return Conversion.BOOL_TO_DATE; // We emit an int here which is ok because of Java's casting rules
        }
        if (from.isString()) {
            return Conversion.STRING_TO_DATE;
        }
        return null;
    }

    private static Conversion conversionToBoolean(DataType from) {
        if (from.isNumeric()) {
            return Conversion.NUMERIC_TO_BOOLEAN;
        }
        if (from.isString()) {
            return Conversion.STRING_TO_BOOLEAN;
        }
        if (from == DATE) {
            return Conversion.DATE_TO_BOOLEAN;
        }
        return null;
    }

    public static byte safeToByte(long x) {
        if (x > Byte.MAX_VALUE || x < Byte.MIN_VALUE) {
            throw new SqlIllegalArgumentException("[" + x + "] out of [Byte] range");
        }
        return (byte) x;
    }

    public static short safeToShort(long x) {
        if (x > Short.MAX_VALUE || x < Short.MIN_VALUE) {
            throw new SqlIllegalArgumentException("[" + x + "] out of [Short] range");
        }
        return (short) x;
    }

    public static int safeToInt(long x) {
        if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE) {
            throw new SqlIllegalArgumentException("[" + x + "] out of [Int] range");
        }
        return (int) x;
    }

    public static long safeToLong(double x) {
        if (x > Long.MAX_VALUE || x < Long.MIN_VALUE) {
            throw new SqlIllegalArgumentException("[" + x + "] out of [Long] range");
        }
        return Math.round(x);
    }

    public static Number toInteger(double x, DataType dataType) {
        long l = safeToLong(x);

        switch (dataType) {
            case BYTE:
                return safeToByte(l);
            case SHORT:
                return safeToShort(l);
            case INTEGER:
                return safeToInt(l);
            default:
                return l;
        }
    }

    public static boolean convertToBoolean(String val) {
        String lowVal = val.toLowerCase(Locale.ROOT);
        if (Booleans.isBoolean(lowVal) == false) {
            throw new SqlIllegalArgumentException("cannot cast [" + val + "] to [Boolean]");
        }
        return Booleans.parseBoolean(lowVal);
    }

    /**
     * Converts arbitrary object to the desired data type.
     * <p>
     * Throws SqlIllegalArgumentException if such conversion is not possible
     */
    public static Object convert(Object value, DataType dataType) {
        DataType detectedType = DataTypes.fromJava(value);
        if (detectedType == dataType || value == null) {
            return value;
        }
        return conversionFor(detectedType, dataType).convert(value);
    }

    /**
     * Reference to a data type conversion that can be serialized. Note that the position in the enum
     * is important because it is used for serialization.
     */
    public enum Conversion {
        IDENTITY(Function.identity()),
        NULL(value -> null),
        
        DATE_TO_STRING(Object::toString),
        OTHER_TO_STRING(String::valueOf),

        RATIONAL_TO_LONG(fromDouble(DataTypeConversion::safeToLong)),
        INTEGER_TO_LONG(fromLong(value -> value)),
        STRING_TO_LONG(fromString(Long::valueOf, "Long")),
        DATE_TO_LONG(fromDate(value -> value)),

        RATIONAL_TO_INT(fromDouble(value -> safeToInt(safeToLong(value)))),
        INTEGER_TO_INT(fromLong(DataTypeConversion::safeToInt)),
        BOOL_TO_INT(fromBool(value -> value ? 1 : 0)),
        STRING_TO_INT(fromString(Integer::valueOf, "Int")),
        DATE_TO_INT(fromDate(DataTypeConversion::safeToInt)),

        RATIONAL_TO_SHORT(fromDouble(value -> safeToShort(safeToLong(value)))),
        INTEGER_TO_SHORT(fromLong(DataTypeConversion::safeToShort)),
        BOOL_TO_SHORT(fromBool(value -> value ? (short) 1 : (short) 0)),
        STRING_TO_SHORT(fromString(Short::valueOf, "Short")),
        DATE_TO_SHORT(fromDate(DataTypeConversion::safeToShort)),

        RATIONAL_TO_BYTE(fromDouble(value -> safeToByte(safeToLong(value)))),
        INTEGER_TO_BYTE(fromLong(DataTypeConversion::safeToByte)),
        BOOL_TO_BYTE(fromBool(value -> value ? (byte) 1 : (byte) 0)),
        STRING_TO_BYTE(fromString(Byte::valueOf, "Byte")),
        DATE_TO_BYTE(fromDate(DataTypeConversion::safeToByte)),

        // TODO floating point conversions are lossy but conversions to integer conversions are not. Are we ok with that?
        RATIONAL_TO_FLOAT(fromDouble(value -> (float) value)),
        INTEGER_TO_FLOAT(fromLong(value -> (float) value)),
        BOOL_TO_FLOAT(fromBool(value -> value ? 1f : 0f)),
        STRING_TO_FLOAT(fromString(Float::valueOf, "Float")),
        DATE_TO_FLOAT(fromDate(value -> (float) value)),

        RATIONAL_TO_DOUBLE(fromDouble(Double::valueOf)),
        INTEGER_TO_DOUBLE(fromLong(Double::valueOf)),
        BOOL_TO_DOUBLE(fromBool(value -> value ? 1d : 0d)),
        STRING_TO_DOUBLE(fromString(Double::valueOf, "Double")),
        DATE_TO_DOUBLE(fromDate(Double::valueOf)),

        RATIONAL_TO_DATE(toDate(RATIONAL_TO_LONG)),
        INTEGER_TO_DATE(toDate(INTEGER_TO_LONG)),
        BOOL_TO_DATE(toDate(BOOL_TO_INT)),
        STRING_TO_DATE(fromString(UTC_DATE_FORMATTER::parseDateTime, "Date")),

        NUMERIC_TO_BOOLEAN(fromLong(value -> value != 0)),
        STRING_TO_BOOLEAN(fromString(DataTypeConversion::convertToBoolean, "Boolean")),
        DATE_TO_BOOLEAN(fromDate(value -> value != 0));

        private final Function<Object, Object> converter;

        Conversion(Function<Object, Object> converter) {
            this.converter = converter;
        }

        private static Function<Object, Object> fromDouble(DoubleFunction<Object> converter) {
            return (Object l) -> converter.apply(((Number) l).doubleValue());
        }

        private static Function<Object, Object> fromLong(LongFunction<Object> converter) {
            return (Object l) -> converter.apply(((Number) l).longValue());
        }
        
        private static Function<Object, Object> fromString(Function<String, Object> converter, String to) {
            return (Object value) -> {
                try {
                    return converter.apply(value.toString());
                } catch (NumberFormatException e) {
                    throw new SqlIllegalArgumentException(e, "cannot cast [{}] to [{}]", value, to);
                } catch (IllegalArgumentException e) {
                    throw new SqlIllegalArgumentException(e, "cannot cast [{}] to [{}]:{}", value, to, e.getMessage());
                }
            };
        }

        private static Function<Object, Object> fromBool(Function<Boolean, Object> converter) {
            return (Object l) -> converter.apply(((Boolean) l));
        }
        
        private static Function<Object, Object> fromDate(Function<Long, Object> converter) {
            return l -> ((ReadableInstant) l).getMillis();
        }

        private static Function<Object, Object> toDate(Conversion conversion) {
            return l -> new DateTime(((Number) conversion.convert(l)).longValue(), DateTimeZone.UTC);
        }

        public Object convert(Object l) {
            if (l == null) {
                return null;
            }
            return converter.apply(l);
        }
    }

    public static DataType asInteger(DataType dataType) {
        if (!dataType.isNumeric()) {
            return dataType;
        }

        return dataType.isInteger ? dataType : LONG;
    }
}