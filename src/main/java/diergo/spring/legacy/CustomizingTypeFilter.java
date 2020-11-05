package diergo.spring.legacy;

import java.lang.reflect.Member;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Support class to combine type filtering, bean definition filtering and customizing as needed by the post processor.
 * A type filter creating bean definitions for visible members declared static on class level.
 *
 * @see MemberPredicates#visible()
 * @see MemberPredicates#atClass()
 * @see LegacyBeanRegistryPostProcessorBuilder
 */
abstract class CustomizingTypeFilter<T extends Member> implements TypeFilter, SmartBeanDefinitionCustomizer {

    protected final Predicate<? super T> accessCheck;

    CustomizingTypeFilter(Predicate<? super T> accessCheck) {
        this.accessCheck = MemberPredicates.<T>visible()
                .and(MemberPredicates.atClass())
                .and(accessCheck);
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        return getAccess(metadataReader.getClassMetadata().getClassName())
                .isPresent();
    }

    @Override
    public boolean supports(BeanDefinition bd) {
        return getAccess(bd.getBeanClassName())
                .isPresent();
    }

    @Override
    public void customize(BeanDefinition bd) {
        getAccess(bd.getBeanClassName())
                .ifPresent(access -> customizeBeanDefinition(access, bd));
    }

    private Optional<T> getAccess(String className) {
        return getType(className).flatMap(this::getAccess);
    }

    protected abstract Optional<T> getAccess(Class<?> type);

    protected abstract void customizeBeanDefinition(T access, BeanDefinition bd);

    static Optional<Class<?>> getType(String className) {
        try {
            return Optional.of(Class.forName(className, false, CustomizingTypeFilter.class.getClassLoader()));
        } catch (ClassNotFoundException | LinkageError e) {
            return Optional.empty();
        }
    }
}
