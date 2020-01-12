package diergo.spring.legacy;

import example.legacy.LegacySingletonByField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

public class LegacySingletonFieldFilterTest {

    private static final MetadataReaderFactory EXAMPLE_FACTORY = new SimpleMetadataReaderFactory();

    private LegacySingletonFieldFilter tested = new LegacySingletonFieldFilter();

    @Test
    public void singletonWithStaticFieldMatches() {
        assertThat(matchTypeFilter(LegacySingletonByField.class), is(true));
        assertThat(matchBeanDefinition(LegacySingletonByField.class), is(true));
    }

    @Test
    public void rootBeanDefinitionOfSingletonWithStaticFieldGetAnInstanceSupplierOnCustomize() {
        RootBeanDefinition actual = new RootBeanDefinition(LegacySingletonByField.class);

        tested.customize(actual);

        assertThat(actual.getScope(), is(SCOPE_SINGLETON));
        assertThat(actual.isLazyInit(), is(true));
        assertThat(actual.getInstanceSupplier().get(), isA(LegacySingletonByField.class));
    }

    @Test
    public void scannedBbeanDefinitionOfSingletonWithStaticFieldGetAnInstanceSupplierOnCustomize() {
        ScannedGenericBeanDefinition actual = new ScannedGenericBeanDefinition(new TestMetadataReader(LegacySingletonByField.class));

        tested.customize(actual);

        assertThat(actual.getScope(), is(SCOPE_SINGLETON));
        assertThat(actual.isLazyInit(), is(true));
        assertThat(actual.getInstanceSupplier().get(), isA(LegacySingletonByField.class));
    }

    @Test
    public void missingStaticFieldDoesNotMatch() {
        assertThat(matchTypeFilter(NonSingletonBean.class), is(false));
        assertThat(matchBeanDefinition(NonSingletonBean.class), is(false));
    }

    private boolean matchTypeFilter(Class<?> type) {
        return tested.match(new TestMetadataReader(type), EXAMPLE_FACTORY);
    }

    private boolean matchBeanDefinition(Class<?> type) {
        return tested.match(new RootBeanDefinition(type));
    }
}