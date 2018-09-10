package org.jdbi.v3.core;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import org.jdbi.v3.core.qualifier.NVarchar;
import org.junit.Test;

public class TestStuff {
    @Test
    public void foo() throws Exception {
        Method method = getClass().getMethod("listOfNVarcharString");
        AnnotatedType returnType = method.getAnnotatedReturnType();

        System.out.println(returnType);
    }

    public List<@NVarchar String> listOfNVarcharString() {
        return null;
    }
}
