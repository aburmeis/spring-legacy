package diergo.spring.legacy;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

public class LegacyBeanRegistryPostProcessorBuilder {

    private static final Pattern GETTERS = Pattern.compile("get[A-Z].+");
    private static final Pattern CONSTANTS = Pattern.compile("[A-Z][A-Z0-9_]+");

    /**
     * Build a post processor scanning the base packages passed.
     * If no packages are passed, the package of the caller is used.
     */
    public static LegacyBeanRegistryPostProcessorBuilder legacyPackages(String... basePackages) {
        if (basePackages.length == 0) {
            StackTraceElement caller = new Throwable().getStackTrace()[1];
            return new LegacyBeanRegistryPostProcessorBuilder(ClassUtils.getPackageName(caller.getClassName()));
        }
        return new LegacyBeanRegistryPostProcessorBuilder(basePackages);
    }

    private final String[] basePackages;
    private final List<CustomizingTypeFilter<?>> included = new ArrayList<>();
    private BeanNameGenerator beanNameGenerator = BeanDefinitionReaderUtils::generateBeanName;
    private int order = Ordered.LOWEST_PRECEDENCE;

    private LegacyBeanRegistryPostProcessorBuilder(String... basePackages) {
        this.basePackages = basePackages;
    }

    /**
     * Use a different bean name generator.
     *
     * @see BeanDefinitionReaderUtils#generateBeanName(BeanDefinition, BeanDefinitionRegistry)
     */
    public LegacyBeanRegistryPostProcessorBuilder beanNaming(BeanNameGenerator beanNameGenerator) {
        this.beanNameGenerator = beanNameGenerator;
        return this;
    }

    /**
     * Adjust the order of the post processor.
     *
     * @see org.springframework.core.PriorityOrdered
     */
    public LegacyBeanRegistryPostProcessorBuilder ordered(int order) {
        this.order = order;
        return this;
    }

    public SingletonBuilder singletonsFrom() {
        return new SingletonBuilder();
    }

    public PrototypeBuilder prototypesFrom() {
        return new PrototypeBuilder();
    }

    /**
     * Create the post processor as configured by the builder.
     * If neither {@link #singletonsFrom()} nor {@link #prototypesFrom()} has been called
     * any static methods and fields will be added as singletons.
     *
     * @see org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
     */
    public LegacyBeanRegistryPostProcessor build() {
        if (included.isEmpty()) {
            included.add(new LegacyBeanMethodFilter(SCOPE_SINGLETON, anyGetter()));
            included.add(new LegacySingletonFieldFilter(andConstant()));
        }
        return new LegacyBeanRegistryPostProcessor(included, beanNameGenerator, order, basePackages);
    }

    public abstract class Builder {

        protected LegacyBeanRegistryPostProcessorBuilder addIncluded(CustomizingTypeFilter<?> filter) {
            LegacyBeanRegistryPostProcessorBuilder.this.included.add(filter);
            return LegacyBeanRegistryPostProcessorBuilder.this;
        }
    }

    public class SingletonBuilder extends Builder {

        public LegacyBeanRegistryPostProcessorBuilder fields(Predicate<? super Field> fieldCheck) {
            return addIncluded(new LegacySingletonFieldFilter(fieldCheck));
        }

        public LegacyBeanRegistryPostProcessorBuilder methods(Predicate<? super Method> memberCheck) {
            return addIncluded(new LegacyBeanMethodFilter(SCOPE_SINGLETON, memberCheck));
        }
    }

    public class PrototypeBuilder extends Builder {

        public LegacyBeanRegistryPostProcessorBuilder methods(Predicate<? super Method> memberCheck) {
            return addIncluded(new LegacyBeanMethodFilter(SCOPE_PROTOTYPE, memberCheck));
        }
    }

    public static Predicate<Member> all() {
        return member -> true;
    }

    public static Predicate<Member> named(String... names) {
        return member -> asList(names).contains(member.getName());
    }

    public static Predicate<Member> named(Pattern name) {
        return member -> name.matcher(member.getName()).matches();
    }

    public static Predicate<Method> anyGetter() {
        return field -> GETTERS.matcher(field.getName()).matches();
    }

    public static Predicate<Field> andConstant() {
        return field -> CONSTANTS.matcher(field.getName()).matches();
    }
}
