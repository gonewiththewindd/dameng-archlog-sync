package org.gone.dameng.autogen.generator;

import com.baomidou.mybatisplus.annotation.TableId;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AutoGenerator {

    public static final Random RANDOM = new Random(47);
    public static final String TEXT = "十分钟后他回来了，手持祖父嗜血的长矛。村里一半的人都聚集在斗鸡场门口，普鲁邓希奥·阿基拉尔正在那里等着。他还没来得及反抗，何塞·阿尔卡蒂奥·布恩迪亚就以公牛般的力气投出了长矛，以第一代奥雷里亚诺·布恩迪亚当年猎杀本地老虎的准头，刺穿了他的咽喉。那天晚上，人们在斗鸡场守灵的时候，何塞·阿尔卡蒂奥·布恩迪亚走进卧室，他妻子正在穿贞节裤。他用长矛指着她，命令道：“脱了";
    public static final String LOCAL_IMAGE_DIR = "";

    public static final Map<String, Byte> classTypeMap = new HashMap() {{
        put(boolean.class.getName(), DataTypeConstants.BOOLEAN);
        put(Boolean.class.getName(), DataTypeConstants.BOOLEAN);

        put(byte.class.getName(), DataTypeConstants.BYTE);
        put(Byte.class.getName(), DataTypeConstants.BYTE);

        put(char.class.getName(), DataTypeConstants.CHAR);
        put(Character.class.getName(), DataTypeConstants.CHAR);

        put(short.class.getName(), DataTypeConstants.SHORT);
        put(Short.class.getName(), DataTypeConstants.SHORT);

        put(int.class.getName(), DataTypeConstants.INT);
        put(Integer.class.getName(), DataTypeConstants.INT);

        put(long.class.getName(), DataTypeConstants.LONG);
        put(Long.class.getName(), DataTypeConstants.LONG);

        put(float.class.getName(), DataTypeConstants.FLOAT);
        put(Float.class.getName(), DataTypeConstants.FLOAT);

        put(double.class.getName(), DataTypeConstants.DOUBLE);
        put(Double.class.getName(), DataTypeConstants.DOUBLE);

        put(String.class.getName(), DataTypeConstants.STRING);

        put(byte[].class.getName(), DataTypeConstants.BYTE_ARRAY);
        put(BigDecimal.class.getName(), DataTypeConstants.BIG_DECIMAL);
        put(LocalDate.class.getName(), DataTypeConstants.LOCAL_DATE);
    }};

    public static <T> T generate(Class<T> clazz) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                TableId annotation = field.getAnnotation(TableId.class);
                if (Objects.nonNull(annotation)) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, randomValue(field));
            }
            return (T) instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> generate(Class<T> clazz, int size) {
        return IntStream.range(0, size).mapToObj(i -> generate(clazz)).collect(Collectors.toList());
    }

    private static Object randomValue(Field field) {

        Byte dataType = classTypeMap.get(field.getType().getName());
        if (Objects.isNull(dataType)) {
            dataType = DataTypeConstants.OBJECT_JSON;
        }
        switch (dataType) {
            case DataTypeConstants.BOOLEAN:
                return RANDOM.nextBoolean();
            case DataTypeConstants.BYTE:
            case DataTypeConstants.CHAR:
                return RANDOM.nextInt(255);
            case DataTypeConstants.SHORT:
                return RANDOM.nextInt(Short.MAX_VALUE);
            case DataTypeConstants.INT:
                return RANDOM.nextInt(Integer.MAX_VALUE);
            case DataTypeConstants.LONG:
                return RANDOM.nextLong(Long.MAX_VALUE);
            case DataTypeConstants.FLOAT:
                return RANDOM.nextFloat();
            case DataTypeConstants.DOUBLE:
                return RANDOM.nextDouble();
            case DataTypeConstants.BIG_DECIMAL:
                return BigDecimal.valueOf(RANDOM.nextDouble());
            case DataTypeConstants.LOCAL_DATE:
                return LocalDate.now().minusDays(RANDOM.nextInt(31));
            case DataTypeConstants.STRING:
                int i = RANDOM.nextInt(0, TEXT.length() - 32);
                return TEXT.substring(i, i + 32);
            case DataTypeConstants.BYTE_ARRAY:
                // TODO 随机本地图片
                return null;
            default:
                throw new RuntimeException("Unsupported data type: " + dataType);
        }
    }


}
