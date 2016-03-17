package org.ed;


import javax.ws.rs.*;
import javax.ws.rs.container.Suspended;
import java.lang.annotation.Annotation;

public class AnnotationProxies {

    private AnnotationProxies() {
    }

    public static GET GET = new GetImpl();

    public static Path Path(String path) {
        return new PathImpl(path);
    }

    public static Produces Produces(String... produced) {
        return new ProducesImpl(produced);
    }

    public static PathParam PathParam(String value){
        return new PathParamImpl(value);
    }

    public static QueryParam QueryParam(String value){
        return new QueryParamImpl(value);
    }

    public static Suspended Suspended(){
        return new SuspendedImpl();
    }

    private static class PathImpl implements Path {
        private final String value;

        public PathImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Path.class;
        }
    }

    private static class GetImpl implements GET {

        @Override
        public Class<? extends Annotation> annotationType() {
            return GET.class;
        }
    }

    private static class ProducesImpl implements Produces {

        private final String[] value;

        private ProducesImpl(String... value) {
            this.value = value;
        }

        @Override
        public String[] value() {
            return value.clone();
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Produces.class;
        }
    }

    private static class PathParamImpl implements PathParam {

        private final String value;

        private PathParamImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return PathParam.class;
        }
    }

    private static class QueryParamImpl implements QueryParam {

        private final String value;

        private QueryParamImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return QueryParam.class;
        }
    }

    private static class SuspendedImpl implements Suspended{

        @Override
        public Class<? extends Annotation> annotationType() {
            return Suspended.class;
        }
    }

}
