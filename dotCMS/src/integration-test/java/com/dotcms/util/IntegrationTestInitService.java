package com.dotcms.util;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.config.DotInitializationService;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfigFactory;
import com.dotcms.test.TestUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.SystemProperties;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.mockito.Mockito;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {

    private static IntegrationTestInitService service = new IntegrationTestInitService();

    private static final AtomicBoolean initCompleted = new AtomicBoolean(false);

    static {
        SystemProperties.getProperties();
    }

    private IntegrationTestInitService() {
    }

    public static IntegrationTestInitService getInstance() {
        ByteBuddyFactory.init();
        return service;
    }


    public void init() throws Exception {
        try {
            if (initCompleted.compareAndSet(false, true)) {
                String classpath = System.getProperty("java.class.path");
                String[] classPathValues = classpath.split(File.pathSeparator);
                Logger.info(IntegrationTestInitService.class,"Classpath="+Arrays.toString(classPathValues));

                System.setProperty(TestUtil.DOTCMS_INTEGRATION_TEST, TestUtil.DOTCMS_INTEGRATION_TEST);
                
                
                
                Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
                Awaitility.setDefaultPollDelay(Duration.ZERO);
                Awaitility.setDefaultTimeout(Duration.ONE_MINUTE);

                ConfigTestHelper._setupFakeTestingContext();

                CacheLocator.init();
                FactoryLocator.init();
                APILocator.init();

                //Running the always run startup tasks
                StartupTasksUtil.getInstance().init();

                //For these tests fire the reindex immediately
                Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", false);
                Config.setProperty("ASYNC_COMMIT_LISTENERS", false);
                Config.setProperty("GRAPHQL_SCHEMA_DEBOUNCE_DELAY_MILLIS", 0);

                Config.setProperty("NETWORK_CACHE_FLUSH_DELAY", (long) 0);
                // Init other dotCMS services.
                DotInitializationService.getInstance().initialize();
            }
        } catch (Exception e) {
            Logger.error(this, "Error initializing Integration Test Init Service", e);
            throw e;
        }
    }

    public void mockStrutsActionModule() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig("");
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
    }
    
    
}
