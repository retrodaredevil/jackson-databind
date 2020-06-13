package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Container class for deserializers that handle core JDK primitive
 * (and matching wrapper) types, as well as standard "big" numeric types.
 * Note that this includes types such as {@link java.lang.Boolean}
 * and {@link java.lang.Character} which are not strictly numeric,
 * but are part of primitive/wrapper types.
 */
public class NumberDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        // note: can skip primitive types; other ways to check them:
        Class<?>[] numberTypes = new Class<?>[] {
            Boolean.class,
            Byte.class,
            Short.class,
            Character.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            // and more generic ones
            Number.class, BigDecimal.class, BigInteger.class
        };
        for (Class<?> cls : numberTypes) {
            _classNames.add(cls.getName());
        }
    }

    public static JsonDeserializer<?> find(Class<?> rawType, String clsName) {
        if (rawType.isPrimitive()) {
            if (rawType == Integer.TYPE) {
                return IntegerDeserializer.primitiveInstance;
            }
            if (rawType == Boolean.TYPE) {
                return BooleanDeserializer.primitiveInstance;
            }
            if (rawType == Long.TYPE) {
                return LongDeserializer.primitiveInstance;
            }
            if (rawType == Double.TYPE) {
                return DoubleDeserializer.primitiveInstance;
            }
            if (rawType == Character.TYPE) {
                return CharacterDeserializer.primitiveInstance;
            }
            if (rawType == Byte.TYPE) {
                return ByteDeserializer.primitiveInstance;
            }
            if (rawType == Short.TYPE) {
                return ShortDeserializer.primitiveInstance;
            }
            if (rawType == Float.TYPE) {
                return FloatDeserializer.primitiveInstance;
            }
            // [databind#2679]: bit odd place for this (Void.class handled in
            // `JdkDeserializers`), due to `void` being primitive type
            if (rawType == Void.TYPE) {
                return NullifyingDeserializer.instance;
            }
        } else if (_classNames.contains(clsName)) {
            // Start with most common types; int, boolean, long, double
            if (rawType == Integer.class) {
                return IntegerDeserializer.wrapperInstance;
            }
            if (rawType == Boolean.class) {
                return BooleanDeserializer.wrapperInstance;
            }
            if (rawType == Long.class) {
                return LongDeserializer.wrapperInstance;
            }
            if (rawType == Double.class) {
                return DoubleDeserializer.wrapperInstance;
            }
            if (rawType == Character.class) {
                return CharacterDeserializer.wrapperInstance;
            }
            if (rawType == Byte.class) {
                return ByteDeserializer.wrapperInstance;
            }
            if (rawType == Short.class) {
                return ShortDeserializer.wrapperInstance;
            }
            if (rawType == Float.class) {
                return FloatDeserializer.wrapperInstance;
            }
            if (rawType == Number.class) {
                return NumberDeserializer.instance;
            }
            if (rawType == BigDecimal.class) {
                return BigDecimalDeserializer.instance;
            }
            if (rawType == BigInteger.class) {
                return BigIntegerDeserializer.instance;
            }
        } else {
            return null;
        }
        // should never occur
        throw new IllegalArgumentException("Internal error: can't find deserializer for "+rawType.getName());
    }

    /*
    /**********************************************************
    /* Then one intermediate base class for things that have
    /* both primitive and wrapper types
    /**********************************************************
     */

    protected abstract static class PrimitiveOrWrapperDeserializer<T>
        extends StdScalarDeserializer<T>
    {
        private static final long serialVersionUID = 1L;

        // @since 2.12
        protected final LogicalType _logicalType;

        protected final T _nullValue;

        // @since 2.9
        protected final T _emptyValue;

        protected final boolean _primitive;

        // @since 2.12
        protected PrimitiveOrWrapperDeserializer(Class<T> vc, LogicalType logicalType,
                T nvl, T empty) {
            super(vc);
            _logicalType = logicalType;
            _nullValue = nvl;
            _emptyValue = empty;
            _primitive = vc.isPrimitive();
        }

        @Deprecated // since 2.12
        protected PrimitiveOrWrapperDeserializer(Class<T> vc, T nvl, T empty) {
            this(vc, LogicalType.OtherScalar, nvl, empty);
        }

        @Override
        public AccessPattern getNullAccessPattern() {
            // 02-Feb-2017, tatu: For primitives we must dynamically check (and possibly throw
            //     exception); for wrappers not.
            if (_primitive) {
                return AccessPattern.DYNAMIC;
            }
            if (_nullValue == null) {
                return AccessPattern.ALWAYS_NULL;
            }
            return AccessPattern.CONSTANT;
        }

        @Override
        public final T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            // 01-Mar-2017, tatu: Alas, not all paths lead to `_coerceNull()`, as `SettableBeanProperty`
            //    short-circuits `null` handling. Hence need this check as well.
            if (_primitive && ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
                ctxt.reportInputMismatch(this,
                        "Cannot map `null` into type %s (set DeserializationConfig.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES to 'false' to allow)",
                        handledType().toString());
            }
            return _nullValue;
        }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
            return _emptyValue;
        }

        @Override // since 2.12
        public final LogicalType logicalType() {
            return _logicalType;
        }
    }

    /*
    /**********************************************************
    /* Then primitive/wrapper types
    /**********************************************************
     */

    @JacksonStdImpl
    public final static class BooleanDeserializer
        extends PrimitiveOrWrapperDeserializer<Boolean>
    {
        private static final long serialVersionUID = 1L;

        final static BooleanDeserializer primitiveInstance = new BooleanDeserializer(Boolean.TYPE, Boolean.FALSE);
        final static BooleanDeserializer wrapperInstance = new BooleanDeserializer(Boolean.class, null);

        public BooleanDeserializer(Class<Boolean> cls, Boolean nvl)
        {
            super(cls, LogicalType.Boolean, nvl, Boolean.FALSE);
        }

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (_primitive) {
                return _parseBooleanPrimitive(ctxt, p);
            }
            return _parseBoolean(ctxt, p, _valueClass);
        }

        // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Boolean deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException
        {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (_primitive) {
                return _parseBooleanPrimitive(ctxt, p);
            }
            return _parseBoolean(ctxt, p, _valueClass);
        }
    }

    @JacksonStdImpl
    public static class ByteDeserializer
        extends PrimitiveOrWrapperDeserializer<Byte>
    {
        private static final long serialVersionUID = 1L;

        final static ByteDeserializer primitiveInstance = new ByteDeserializer(Byte.TYPE, (byte) 0);
        final static ByteDeserializer wrapperInstance = new ByteDeserializer(Byte.class, null);

        public ByteDeserializer(Class<Byte> cls, Byte nvl)
        {
            super(cls, LogicalType.Integer, nvl, (byte) 0);
        }

        @Override
        public Byte deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return p.getByteValue();
            }
            if (_primitive) {
                return _parseBytePrimitive(ctxt, p);
            }
            return _parseByte(ctxt, p);
        }

        protected Byte _parseByte(DeserializationContext ctxt, JsonParser p)
                throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                String text = p.getText();
                act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Byte) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Byte) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Byte) _coerceTextualNull(ctxt, false);
                }
                int value;
                try {
                    value = NumberInput.parseInt(text);
                } catch (IllegalArgumentException iae) {
                    return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                            "not a valid Byte value");
                }
                // So far so good: but does it fit?
                // as per [JACKSON-804], allow range up to 255, inclusive
                if (_byteOverflow(value)) {
                    return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                            "overflow, value cannot be represented as 8-bit value");
                    // fall-through for deferred fails
                }
                return Byte.valueOf((byte) value);
            case JsonTokenId.ID_NUMBER_FLOAT:
                act = _checkFloatToIntCoercion(ctxt, p, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (Byte) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Byte) getEmptyValue(ctxt);
                }
                return p.getByteValue();
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Byte) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_INT:
                return p.getByteValue();
            case JsonTokenId.ID_START_ARRAY:
                return (Byte) _deserializeFromArray(p, ctxt);
            }
            return (Byte) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
    }

    @JacksonStdImpl
    public static class ShortDeserializer
        extends PrimitiveOrWrapperDeserializer<Short>
    {
        private static final long serialVersionUID = 1L;

        final static ShortDeserializer primitiveInstance = new ShortDeserializer(Short.TYPE, Short.valueOf((short)0));
        final static ShortDeserializer wrapperInstance = new ShortDeserializer(Short.class, null);
        
        public ShortDeserializer(Class<Short> cls, Short nvl)
        {
            super(cls, LogicalType.Integer, nvl, (short)0);
        }

        @Override
        public Short deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return p.getShortValue();
            }
            if (_primitive) {
                return _parseShortPrimitive(ctxt, p);
            }
            return _parseShort(ctxt, p);
        }

        protected Short _parseShort(DeserializationContext ctxt, JsonParser p)
                throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                String text = p.getText();
                act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Short) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Short) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Short) _coerceTextualNull(ctxt, false);
                }
                int value;
                try {
                    value = NumberInput.parseInt(text);
                } catch (IllegalArgumentException iae) {
                    return (Short) ctxt.handleWeirdStringValue(_valueClass, text,
                            "not a valid Short value");
                }
                // So far so good: but does it fit?
                if (_shortOverflow(value)) {
                    return (Short) ctxt.handleWeirdStringValue(_valueClass, text,
                            "overflow, value cannot be represented as 16-bit value");
                }
                return (short) value;
            case JsonTokenId.ID_NUMBER_FLOAT:
                act = _checkFloatToIntCoercion(ctxt, p, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (Short) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Short) getEmptyValue(ctxt);
                }
                return p.getShortValue();
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Short) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_INT:
                return p.getShortValue();
            case JsonTokenId.ID_START_ARRAY:
                return (Short)_deserializeFromArray(p, ctxt);
            }
            return (Short) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
    }

    @JacksonStdImpl
    public static class CharacterDeserializer
        extends PrimitiveOrWrapperDeserializer<Character>
    {
        private static final long serialVersionUID = 1L;

        final static CharacterDeserializer primitiveInstance = new CharacterDeserializer(Character.TYPE, '\0');
        final static CharacterDeserializer wrapperInstance = new CharacterDeserializer(Character.class, null);
        
        public CharacterDeserializer(Class<Character> cls, Character nvl)
        {
            super(cls,
                    // 07-Jun-2020, tatu: Debatable if it should be "OtherScalar" or Integer but...
                    LogicalType.Integer, nvl, '\0');
        }

        @Override
        public Character deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT: // ok iff Unicode value
                // 12-Jun-2020, tatu: inlined from `StdDeserializer`
                if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
                    ctxt.reportInputMismatch(this,
"Cannot coerce Integer value to `Character` (enable `MapperFeature.ALLOW_COERCION_OF_SCALARS` to allow)");
                }
                int value = p.getIntValue();
                if (value >= 0 && value <= 0xFFFF) {
                    return Character.valueOf((char) value);
                }
                break;
            case JsonTokenId.ID_STRING: // this is the usual type

                String text = p.getText();
                CoercionAction act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Character) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Character) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Character) _coerceTextualNull(ctxt, _primitive);
                }
                // But does it have to be exactly one char?
                if (text.length() == 1) {
                    return Character.valueOf(text.charAt(0));
                }
                break;
            case JsonTokenId.ID_NULL:
                if (_primitive) {
                    _verifyNullForPrimitive(ctxt);
                }
                return (Character) getNullValue(ctxt);
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
            }
            return (Character) ctxt.handleUnexpectedToken(_valueClass, p);
        }
    }

    @JacksonStdImpl
    public final static class IntegerDeserializer
        extends PrimitiveOrWrapperDeserializer<Integer>
    {
        private static final long serialVersionUID = 1L;

        final static IntegerDeserializer primitiveInstance = new IntegerDeserializer(Integer.TYPE, 0);
        final static IntegerDeserializer wrapperInstance = new IntegerDeserializer(Integer.class, null);
        
        public IntegerDeserializer(Class<Integer> cls, Integer nvl) {
            super(cls, LogicalType.Integer, nvl, 0);
        }

        // since 2.6, slightly faster lookups for this very common type
        @Override
        public boolean isCachable() { return true; }

        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return p.getIntValue();
            }
            if (_primitive) {
                return _parseIntPrimitive(ctxt, p);
            }
            return _parseInteger(ctxt, p);
        }

        // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Integer deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer) throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return p.getIntValue();
            }
            if (_primitive) {
                return _parseIntPrimitive(ctxt, p);
            }
            return _parseInteger(ctxt, p);
        }

        // @since 2.12
        protected final Integer _parseInteger(DeserializationContext ctxt, JsonParser p)
                throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                String text = p.getText();
                act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Integer) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Integer) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Integer) _coerceTextualNull(ctxt, false);
                }
                final int len = text.length();
                try {
                    if (len > 9) {
                        long l = Long.parseLong(text);
                        if (_intOverflow(l)) {
                            return (Integer) ctxt.handleWeirdStringValue(_valueClass, text, String.format(
                                "Overflow: numeric value (%s) out of range of Integer (%d - %d)",
                                text, Integer.MIN_VALUE, Integer.MAX_VALUE));
                        }
                        return Integer.valueOf((int) l);
                    }
                    return Integer.valueOf(NumberInput.parseInt(text));
                } catch (IllegalArgumentException iae) {
                    return (Integer) ctxt.handleWeirdStringValue(_valueClass, text,
                            "not a valid Integer value");
                }
            case JsonTokenId.ID_NUMBER_FLOAT: // coercing may work too
                act = _checkFloatToIntCoercion(ctxt, p, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (Integer) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Integer) getEmptyValue(ctxt);
                }
                return p.getValueAsInt();
            case JsonTokenId.ID_NUMBER_INT: // NOTE: caller assumed to check in fast path
                return p.getIntValue();
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Integer) getNullValue(ctxt);
            case JsonTokenId.ID_START_ARRAY:
                return (Integer) _deserializeFromArray(p, ctxt);
            }
            return (Integer) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
    }

    @JacksonStdImpl
    public final static class LongDeserializer
        extends PrimitiveOrWrapperDeserializer<Long>
    {
        private static final long serialVersionUID = 1L;

        final static LongDeserializer primitiveInstance = new LongDeserializer(Long.TYPE, 0L);
        final static LongDeserializer wrapperInstance = new LongDeserializer(Long.class, null);
        
        public LongDeserializer(Class<Long> cls, Long nvl) {
            super(cls, LogicalType.Integer, nvl, 0L);
        }

        // since 2.6, slightly faster lookups for this very common type
        @Override
        public boolean isCachable() { return true; }
        
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return p.getLongValue();
            }
            if (_primitive) {
                return _parseLongPrimitive(ctxt, p);
            }
            return _parseLong(ctxt, p);
        }

        // @since 2.12
        protected final Long _parseLong(DeserializationContext ctxt, JsonParser p)
                throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                String text = p.getText();
                act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Long) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Long) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Long) _coerceTextualNull(ctxt, false);
                }
                // let's allow Strings to be converted too
                try {
                    return Long.valueOf(NumberInput.parseLong(text));
                } catch (IllegalArgumentException iae) { }
                return (Long) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Long value");
            case JsonTokenId.ID_NUMBER_FLOAT:
                act = _checkFloatToIntCoercion(ctxt, p, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (Long) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Long) getEmptyValue(ctxt);
                }
                return p.getValueAsLong();
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Long) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_INT:
                return p.getLongValue();
            case JsonTokenId.ID_START_ARRAY:
                return (Long) _deserializeFromArray(p, ctxt);
            }
            // Otherwise, no can do:
            return (Long) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
    }

    @JacksonStdImpl
    public static class FloatDeserializer
        extends PrimitiveOrWrapperDeserializer<Float>
    {
        private static final long serialVersionUID = 1L;

        final static FloatDeserializer primitiveInstance = new FloatDeserializer(Float.TYPE, 0.f);
        final static FloatDeserializer wrapperInstance = new FloatDeserializer(Float.class, null);
        
        public FloatDeserializer(Class<Float> cls, Float nvl) {
            super(cls, LogicalType.Float, nvl, 0.f);
        }

        @Override
        public Float deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return p.getFloatValue();
            }
            if (_primitive) {
                return _parseFloatPrimitive(ctxt, p);
            }
            return _parseFloat(p, ctxt);
        }

        protected final Float _parseFloat(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                String text = p.getText();
                 act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Float) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Float) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Float) _coerceTextualNull(ctxt, false);
                }
                switch (text.charAt(0)) {
                case 'I':
                    if (_isPosInf(text)) {
                        return Float.POSITIVE_INFINITY;
                    }
                    break;
                case 'N':
                    if (_isNaN(text)) {
                        return Float.NaN;
                    }
                    break;
                case '-':
                    if (_isNegInf(text)) {
                        return Float.NEGATIVE_INFINITY;
                    }
                    break;
                }
                try {
                    return Float.parseFloat(text);
                } catch (IllegalArgumentException iae) { }
                return (Float) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Float value");
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Float) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_FLOAT:
            case JsonTokenId.ID_NUMBER_INT: // safe coercion
                return p.getFloatValue();
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            }
            // Otherwise, no can do:
            return (Float) ctxt.handleUnexpectedToken(_valueClass, p);
        }
    }

    @JacksonStdImpl
    public static class DoubleDeserializer
        extends PrimitiveOrWrapperDeserializer<Double>
    {
        private static final long serialVersionUID = 1L;

        final static DoubleDeserializer primitiveInstance = new DoubleDeserializer(Double.TYPE, 0.d);
        final static DoubleDeserializer wrapperInstance = new DoubleDeserializer(Double.class, null);
        
        public DoubleDeserializer(Class<Double> cls, Double nvl) {
            super(cls, LogicalType.Float, nvl, 0.d);
        }

        @Override
        public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return p.getDoubleValue();
            }
            if (_primitive) {
                return _parseDoublePrimitive(ctxt, p);
            }
            return _parseDouble(p, ctxt);
        }

        // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Double deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer) throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return p.getDoubleValue();
            }
            if (_primitive) {
                return _parseDoublePrimitive(ctxt, p);
            }
            return _parseDouble(p, ctxt);
        }

        protected final Double _parseDouble(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                String text = p.getText();
                act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return (Double) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Double) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    return (Double) _coerceTextualNull(ctxt, _primitive);
                }
                switch (text.charAt(0)) {
                case 'I':
                    if (_isPosInf(text)) {
                        return Double.POSITIVE_INFINITY;
                    }
                    break;
                case 'N':
                    if (_isNaN(text)) {
                        return Double.NaN;
                    }
                    break;
                case '-':
                    if (_isNegInf(text)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    break;
                }
                try {
                    return parseDouble(text);
                } catch (IllegalArgumentException iae) { }
                return (Double) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Double value");
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Double) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_FLOAT:
            case JsonTokenId.ID_NUMBER_INT: // safe coercion
                return p.getDoubleValue();
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            }
            return (Double) ctxt.handleUnexpectedToken(_valueClass, p);
        }
    }

    /**
     * For type <code>Number.class</code>, we can just rely on type
     * mappings that plain {@link JsonParser#getNumberValue} returns.
     *<p>
     * There is one additional complication: some numeric
     * types (specifically, int/Integer and double/Double) are "non-typed";
     * meaning that they will NEVER be output with type information.
     * But other numeric types may need such type information.
     * This is why {@link #deserializeWithType} must be overridden.
     */
    @SuppressWarnings("serial")
    @JacksonStdImpl
    public static class NumberDeserializer
        extends StdScalarDeserializer<Object>
    {
        public final static NumberDeserializer instance = new NumberDeserializer();
        
        public NumberDeserializer() {
            super(Number.class);
        }

        @Override // since 2.12
        public final LogicalType logicalType() {
            // 07-Jun-2020, tatu: Hmmh... tricky choice. For now, use:
            return LogicalType.Integer;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT:
                if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                    return _coerceIntegral(p, ctxt);
                }
                return p.getNumberValue();

            case JsonTokenId.ID_NUMBER_FLOAT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    // 10-Mar-2017, tatu: NaN and BigDecimal won't mix...
                    if (!p.isNaN()) {
                        return p.getDecimalValue();
                    }
                }
                return p.getNumberValue();

            case JsonTokenId.ID_STRING:
                // Textual values are more difficult... not parsing itself, but figuring
                // out 'minimal' type to use 

                String text = p.getText();
                CoercionAction act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    // note: no need to call `coerce` as this is never primitive
                    return getNullValue(ctxt);
                }
                if (_isPosInf(text)) {
                    return Double.POSITIVE_INFINITY;
                }
                if (_isNegInf(text)) {
                    return Double.NEGATIVE_INFINITY;
                }
                if (_isNaN(text)) {
                    return Double.NaN;
                }
                try {
                    if (!_isIntNumber(text)) {
                        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                            return new BigDecimal(text);
                        }
                        return Double.valueOf(text);
                    }
                    if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                        return new BigInteger(text);
                    }
                    long value = Long.parseLong(text);
                    if (!ctxt.isEnabled(DeserializationFeature.USE_LONG_FOR_INTS)) {
                        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                            return Integer.valueOf((int) value);
                        }
                    }
                    return Long.valueOf(value);
                } catch (IllegalArgumentException iae) {
                    return ctxt.handleWeirdStringValue(_valueClass, text,
                            "not a valid number");
                }
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            }
            // Otherwise, no can do:
            return ctxt.handleUnexpectedToken(_valueClass, p);
        }

        /**
         * As mentioned in class Javadoc, there is additional complexity in
         * handling potentially mixed type information here. Because of this,
         * we must actually check for "raw" integers and doubles first, before
         * calling type deserializer.
         */
        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException
        {
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT:
            case JsonTokenId.ID_NUMBER_FLOAT:
            case JsonTokenId.ID_STRING:
                // cannot point to type information: hence must be non-typed (int/double)
                return deserialize(p, ctxt);
            }
            return typeDeserializer.deserializeTypedFromScalar(p, ctxt);
        }
    }

    /*
    /**********************************************************
    /* And then bit more complicated (but non-structured) number types
    /**********************************************************
     */

    /**
     * This is bit trickier to implement efficiently, while avoiding
     * overflow problems.
     */
    @SuppressWarnings("serial")
    @JacksonStdImpl
    public static class BigIntegerDeserializer
        extends StdScalarDeserializer<BigInteger>
    {
        public final static BigIntegerDeserializer instance = new BigIntegerDeserializer();

        public BigIntegerDeserializer() { super(BigInteger.class); }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return BigInteger.ZERO;
        }

        @Override // since 2.12
        public final LogicalType logicalType() {
            return LogicalType.Integer;
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            CoercionAction act;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT:
                switch (p.getNumberType()) {
                case INT:
                case LONG:
                case BIG_INTEGER:
                    return p.getBigIntegerValue();
                }
                break;
            case JsonTokenId.ID_NUMBER_FLOAT:
                act = _checkFloatToIntCoercion(ctxt, p, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (BigInteger) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (BigInteger) getEmptyValue(ctxt);
                }
                return p.getDecimalValue().toBigInteger();
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                String text = p.getText();
                act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (BigInteger) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    // note: no need to call `coerce` as this is never primitive
                    return getNullValue(ctxt);
                }
                try {
                    return new BigInteger(text);
                } catch (IllegalArgumentException iae) { }
                return (BigInteger) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid representation");
            }
            // String is ok too, can easily convert; otherwise, no can do:
            return (BigInteger) ctxt.handleUnexpectedToken(_valueClass, p);
        }
    }
    
    @SuppressWarnings("serial")
    @JacksonStdImpl
    public static class BigDecimalDeserializer
        extends StdScalarDeserializer<BigDecimal>
    {
        public final static BigDecimalDeserializer instance = new BigDecimalDeserializer();
 
        public BigDecimalDeserializer() { super(BigDecimal.class); }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return BigDecimal.ZERO;
        }

        @Override // since 2.12
        public final LogicalType logicalType() {
            return LogicalType.Float;
        }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
        {
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT:
            case JsonTokenId.ID_NUMBER_FLOAT:
                return p.getDecimalValue();
            case JsonTokenId.ID_STRING:
                String text = p.getText();
                CoercionAction act = _checkFromStringCoercion(ctxt, text);
                if (act == CoercionAction.AsNull) {
                    return getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (BigDecimal) getEmptyValue(ctxt);
                }
                text = text.trim();
                if (_hasTextualNull(text)) {
                    // note: no need to call `coerce` as this is never primitive
                    return getNullValue(ctxt);
                }
                try {
                    return new BigDecimal(text);
                } catch (IllegalArgumentException iae) { }
                return (BigDecimal) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid representation");
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            }
            // Otherwise, no can do:
            return (BigDecimal) ctxt.handleUnexpectedToken(_valueClass, p);
        }
    }
}
