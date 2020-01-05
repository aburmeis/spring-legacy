package diergo.spring.legacy;

import example.legacy.LegacySingletonByMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LegacySingletonMethodFilterTest {

    private static final MetadataReaderFactory EXAMPLE_FACTORY = new SimpleMetadataReaderFactory();

    private LegacySingletonMethodFilter tested = new LegacySingletonMethodFilter();

    @Test
    public void singletonWithStaticMethodMatches() {
        assertThat(matchTypeFilter(LegacySingletonByMethod.class), is(true));
        assertThat(matchBeanDefinition(LegacySingletonByMethod.class), is(true));
    }

    @Test
    public void beanDefinitionOfSingletonWithStaticMethodWillGetTheFactoryMethodOnCustomize() {
        RootBeanDefinition actual = new RootBeanDefinition(LegacySingletonByMethod.class);

        tested.customize(actual);

        assertThat(actual.getFactoryMethodName(), is("getInstance"));
    }

    @Test
    public void missingStaticMethodDoesNotMatch() {
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
