package com.lingjoin.modelmapper;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class DtoMapperUtilsTest {

    @Getter
    @Setter
    public static class TestEntity {
        String name;
        List<String> names;
        Integer num;

        protected TestEntity() {
        }

        public TestEntity(String name, List<String> names, Integer num) {
            this.name = name;
            this.names = names;
            this.num = num;
        }
    }

    @Getter @Setter
    public static class TestClassDto implements DtoMapperUtils.DtoMapperInterface<TestEntity> {
        String name;
        List<String> names;
    }

    record TestRecordDto(List<String> names, Integer num) implements DtoMapperUtils.DtoMapperInterface<TestEntity> {
    }

    @Test
    void testMapWithoutEntity() {
        TestClassDto testClassDto = new TestClassDto();
        testClassDto.setName("test");
        testClassDto.setNames(List.of("a", "b", "c"));
        TestEntity entity = DtoMapperUtils.map(testClassDto, TestEntity.class);
        assertNull(entity.getNum());
        assertEquals(testClassDto.names, entity.getNames());
        assertEquals(testClassDto.name, entity.getName());
    }

    @Test
    void testMapWithEntity() {
        TestEntity testEntity = new TestEntity("test2", List.of("a", "b", "c"), 1);
        TestClassDto testClassDto = new TestClassDto();
        testClassDto.setName("test");
        testClassDto.setNames(List.of("d", "c"));
        TestEntity entity = DtoMapperUtils.map(testClassDto, testEntity);
        assertEquals(1, entity.getNum());
        assertEquals(testClassDto.names, entity.getNames());
        assertEquals(testClassDto.name, entity.getName());

        // 空不赋值测试
        TestClassDto testClassDto2 = new TestClassDto();
        testClassDto2.setName("test3");
        testClassDto2.setNames(null);
        DtoMapperUtils.map(testClassDto2, testEntity);
        assertEquals(1, testEntity.getNum());
        assertEquals(testClassDto.names, testEntity.getNames());
        assertEquals(testClassDto2.name, testEntity.getName());
    }

    @Test
    void testMapWithRecord() {
        var testEntity = new TestEntity("test2", List.of("a", "b", "c"), 1);
        var names1 = List.of("a", "b");
        var names2 = List.of("c", "d", "r");
        var testRecordDto1 = new TestRecordDto(names1, 2);
        var entity = testRecordDto1.map();
        // test for map without entity
        assertEquals(testRecordDto1.num(), entity.getNum());
        assertNull(entity.getName());
        assertEquals(names1, entity.getNames());
        // test for map with entity
        testRecordDto1.map(testEntity);
        assertEquals(names1, testEntity.getNames());
        assertEquals(testRecordDto1.num(), testEntity.getNum());
        var testRecordDto2 = new TestRecordDto(names2, 3);
        testRecordDto2.map(testEntity);
        assertEquals(names2, testEntity.getNames());
        assertEquals(testRecordDto2.num(), testEntity.getNum());
        // 空不赋值测试
        var testRecordDto3 = new TestRecordDto(null, 4);
        testRecordDto3.map(testEntity);
        assertEquals(names2, testEntity.getNames());
        assertEquals(testRecordDto3.num(), testEntity.getNum());
    }
}