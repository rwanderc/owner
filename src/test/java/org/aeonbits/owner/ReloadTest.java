/*
 * Copyright (c) 2013, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner;

import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.event.ReloadEvent;
import org.aeonbits.owner.event.ReloadListener;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.aeonbits.owner.UtilTest.save;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Luigi R. Viggiano
 */
@RunWith(MockitoJUnitRunner.class)
public class ReloadTest {
    private static final String spec = "file:target/test-resources/ReloadableConfig.properties";
    private static File target;
    @Mock ReloadListener listener;

    @BeforeClass
    public static void beforeClass() throws MalformedURLException {
        target = new File(new URL(spec).getFile());
    }

    @Before
    public void before() throws Throwable {
        save(target, new Properties() {{
            setProperty("minimumAge", "18");
        }});
    }

    @Sources(spec)
    public interface ReloadableConfig extends Config, Reloadable {
        Integer minimumAge();
    }

    @Test
    public void testReload() throws Throwable {
        ReloadableConfig cfg = ConfigFactory.create(ReloadableConfig.class);

        assertEquals(Integer.valueOf(18), cfg.minimumAge());

        save(target, new Properties() {{
            setProperty("minimumAge", "21");
        }});

        cfg.reload();
        assertEquals(Integer.valueOf(21), cfg.minimumAge());
    }

    public interface ReloadImportConfig extends Config, Reloadable {
        Integer minimumAge();
    }

    @Test
    public void testReloadWithImportedProperties() throws Throwable {
        Properties props = new Properties() {{
           setProperty("minimumAge", "18");
        }};

        ReloadImportConfig cfg = ConfigFactory.create(ReloadImportConfig.class, props);
        assertEquals(Integer.valueOf(18), cfg.minimumAge());

        props.setProperty("minimumAge", "21"); // changing props doesn't reflect to cfg immediately
        assertEquals(Integer.valueOf(18), cfg.minimumAge());

        cfg.reload(); // the config gets reloaded, so the change in props gets reflected
        assertEquals(Integer.valueOf(21), cfg.minimumAge());
    }

    @After
    public void after() throws Throwable {
        target.delete();
    }

    @Test
    public void testReloadListener() throws Throwable {
        ReloadableConfig cfg = ConfigFactory.create(ReloadableConfig.class);
        cfg.addReloadListener(listener);
        cfg.reload();
        cfg.reload();
        cfg.reload();
        verify(listener, times(3)).reloadPerformed(argThat(isReloadListnerWithSource(cfg)));
    }

    @Test
    public void testReloadListenerRemoved() throws Throwable {
        ReloadableConfig cfg = ConfigFactory.create(ReloadableConfig.class);
        cfg.addReloadListener(listener);
        cfg.reload();
        cfg.reload();
        cfg.removeReloadListener(listener);
        cfg.reload();
        verify(listener, times(2)).reloadPerformed(argThat(isReloadListnerWithSource(cfg)));
    }

    private Matcher<ReloadEvent> isReloadListnerWithSource(final ReloadableConfig cfg) {
        return new BaseMatcher<ReloadEvent>() {
            public boolean matches(Object o) {
                ReloadEvent given = (ReloadEvent) o;
                return given.getSource() == cfg;
            }

            public void describeTo(Description description) {
                description.appendText("does not match");
            }
        };
    }

}
