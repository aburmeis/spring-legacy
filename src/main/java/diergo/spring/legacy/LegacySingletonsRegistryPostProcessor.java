package diergo.spring.legacy;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A post processor registering all legacy singletons as spring beans.
 * It will check the {@linkplain #scanForSingletons(String...) basePackages} for any types with static
 * {@linkplain Builder#fromStaticMethods(String...) methods} or {@linkplain Builder#fromStaticFields(String...) fields}.
 */
public class LegacySingletonsRegistryPostProcessor extends AbstractRegistryPostProcessor {

    public static Builder scanForSingletons(String... basePackages) {
        if (basePackages.length == 0) {
            StackTraceElement caller = new Throwable().getStackTrace()[1];
            return new Builder(ClassUtils.getPackageName(caller.getClassName()));
        }
        return new Builder(basePackages);
    }

    private final Builder builder;

    private LegacySingletonsRegistryPostProcessor(Builder builder) {
        this.builder = builder;
        setOrder(builder.order);
    }

    @Override
    protected void postProcess(BeanDefinitionRegistry registry) {
        ClassPathBeanDefinitionScanner scanner = new LegacyClassPathBeanDefinitionScanner(registry, false, environment,
                this::customizeBeanDefinition);
        BeanDefinitionDefaults defaults = new BeanDefinitionDefaults();
        defaults.setLazyInit(true);
        scanner.setBeanDefinitionDefaults(defaults);
        scanner.setBeanNameGenerator(builder.beanNameGenerator);
        builder.included.forEach(scanner::addIncludeFilter);
        scanner.scan(builder.basePackages);
    }

    private void customizeBeanDefinition(BeanDefinition bd) {
        builder.included.stream()
                .filter(included -> included.match(bd))
                .findFirst()
                .ifPresent(included -> included.customize(bd));
    }

    public static class Builder {

        private final String[] basePackages;
        private final List<CustomizingTypeFilter<?>> included = new ArrayList<>();
        private BeanNameGenerator beanNameGenerator = BeanDefinitionReaderUtils::generateBeanName;
        private int order = Ordered.LOWEST_PRECEDENCE;

        private Builder(String... basePackages) {
            this.basePackages = basePackages;
        }

        public Builder beanNaming(BeanNameGenerator beanNameGenerator) {
            this.beanNameGenerator = beanNameGenerator;
            return this;
        }

        public Builder ordered(int order) {
            this.order = order;
            return this;
        }

        public Builder fromStaticFields(String... fieldNames) {
            included.add(new LegacySingletonFieldFilter(fieldNames));
            return this;
        }

        public Builder fromStaticMethods(String... methodNames) {
            included.add(new LegacySingletonMethodFilter(methodNames));
            return this;
        }

        public LegacySingletonsRegistryPostProcessor asPostProcessor() {
            if (included.isEmpty()) {
                fromStaticMethods();
                fromStaticFields();
            }
            return new LegacySingletonsRegistryPostProcessor(this);
        }
    }

    private static class LegacyClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

        private final BeanDefinitionCustomizer additionalCustomizer;

        LegacyClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment, BeanDefinitionCustomizer additionalCustomizer) {
            super(registry, useDefaultFilters, environment);
            this.additionalCustomizer = additionalCustomizer;
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            AnnotationMetadata metadata = beanDefinition.getMetadata();
            return metadata.isIndependent() && !metadata.isInterface();
        }

        @Override
        protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
            super.postProcessBeanDefinition(beanDefinition, beanName);
            additionalCustomizer.customize(beanDefinition);
        }
    }
}
