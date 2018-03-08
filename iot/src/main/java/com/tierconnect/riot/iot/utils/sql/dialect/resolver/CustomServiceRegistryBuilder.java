package com.tierconnect.riot.iot.utils.sql.dialect.resolver;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ConfigLoader;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.internal.ProvidedService;

import java.util.*;

/**
 * Created by julio.rocha on 23-06-17.
 */
public class CustomServiceRegistryBuilder {
    /**
     * The default resource name for a hibernate configuration xml file.
     */
    public static final String DEFAULT_CFG_RESOURCE_NAME = "hibernate.cfg.xml";

    private final Map settings;
    private final List<StandardServiceInitiator> initiators = standardInitiatorList();
    private final List<ProvidedService> providedServices = new ArrayList<ProvidedService>();

    private boolean autoCloseRegistry = true;

    private final BootstrapServiceRegistry bootstrapServiceRegistry;
    private final ConfigLoader configLoader;

    /**
     * Create a default builder.
     */
    public CustomServiceRegistryBuilder() {
        this(new BootstrapServiceRegistryBuilder().build());
    }

    /**
     * Create a builder with the specified bootstrap services.
     *
     * @param bootstrapServiceRegistry Provided bootstrap registry to use.
     */
    public CustomServiceRegistryBuilder(BootstrapServiceRegistry bootstrapServiceRegistry) {
        this.settings = Environment.getProperties();
        this.settings.remove("hibernate.dialect");
        this.bootstrapServiceRegistry = bootstrapServiceRegistry;
        this.configLoader = new ConfigLoader(bootstrapServiceRegistry);
    }

    /**
     * Used from the {@link #initiators} variable initializer
     *
     * @return List of standard initiators
     */
    private static List<StandardServiceInitiator> standardInitiatorList() {
        final List<StandardServiceInitiator> initiators = new ArrayList<StandardServiceInitiator>();
        initiators.addAll(StandardServiceInitiators.LIST);
        return initiators;
    }

    public BootstrapServiceRegistry getBootstrapServiceRegistry() {
        return bootstrapServiceRegistry;
    }

    /**
     * Read settings from a {@link java.util.Properties} file by resource name.
     * <p>
     * Differs from {@link #configure()} and {@link #configure(String)} in that here we expect to read a
     * {@link java.util.Properties} file while for {@link #configure} we read the XML variant.
     *
     * @param resourceName The name by which to perform a resource look up for the properties file.
     * @return this, for method chaining
     * @see #configure()
     * @see #configure(String)
     */
    @SuppressWarnings({"unchecked"})
    public CustomServiceRegistryBuilder loadProperties(String resourceName) {
        settings.putAll(configLoader.loadProperties(resourceName));
        return this;
    }

    /**
     * Read setting information from an XML file using the standard resource location.
     *
     * @return this, for method chaining
     * @see #DEFAULT_CFG_RESOURCE_NAME
     * @see #configure(String)
     * @see #loadProperties(String)
     */
    public CustomServiceRegistryBuilder configure() {
        return configure(DEFAULT_CFG_RESOURCE_NAME);
    }

    /**
     * Read setting information from an XML file using the named resource location.
     *
     * @param resourceName The named resource
     * @return this, for method chaining
     * @see #loadProperties(String)
     */
    @SuppressWarnings({"unchecked"})
    public CustomServiceRegistryBuilder configure(String resourceName) {
        final JaxbHibernateConfiguration configurationElement = configLoader.loadConfigXmlResource(resourceName);
        for (JaxbHibernateConfiguration.JaxbSessionFactory.JaxbProperty xmlProperty : configurationElement.getSessionFactory().getProperty()) {
            settings.put(xmlProperty.getName(), xmlProperty.getValue());
        }

        return this;
    }

    /**
     * Apply a setting value.
     *
     * @param settingName The name of the setting
     * @param value       The value to use.
     * @return this, for method chaining
     */
    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    public CustomServiceRegistryBuilder applySetting(String settingName, Object value) {
        settings.put(settingName, value);
        return this;
    }

    /**
     * Apply a groups of setting values.
     *
     * @param settings The incoming settings to apply
     * @return this, for method chaining
     */
    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    public CustomServiceRegistryBuilder applySettings(Map settings) {
        this.settings.putAll(settings);
        return this;
    }

    /**
     * Adds a service initiator.
     *
     * @param initiator The initiator to be added
     * @return this, for method chaining
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public CustomServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
        initiators.add(initiator);
        return this;
    }

    /**
     * Adds a user-provided service.
     *
     * @param serviceRole The role of the service being added
     * @param service     The service implementation
     * @return this, for method chaining
     */
    @SuppressWarnings({"unchecked"})
    public CustomServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
        providedServices.add(new ProvidedService(serviceRole, service));
        return this;
    }

    /**
     * By default, when a ServiceRegistry is no longer referenced by any other
     * registries as a parent it will be closed.
     * <p/>
     * Some applications that explicitly build "shared registries" may want to
     * circumvent that behavior.
     * <p/>
     * This method indicates that the registry being built should not be
     * automatically closed.  The caller agrees to take responsibility to
     * close it themselves.
     *
     * @return this, for method chaining
     */
    public CustomServiceRegistryBuilder disableAutoClose() {
        this.autoCloseRegistry = false;
        return this;
    }

    /**
     * See the discussion on {@link #disableAutoClose}.  This method enables
     * the auto-closing.
     *
     * @return this, for method chaining
     */
    public CustomServiceRegistryBuilder enableAutoClose() {
        this.autoCloseRegistry = true;
        return this;
    }

    /**
     * Build the StandardServiceRegistry.
     *
     * @return The StandardServiceRegistry.
     */
    @SuppressWarnings("unchecked")
    public StandardServiceRegistry build() {
        final Map<?, ?> settingsCopy = new HashMap();
        settings.remove("hibernate.dialect");
        settingsCopy.putAll(settings);
        Environment.verifyProperties(settingsCopy);
        settingsCopy.remove("hibernate.dialect");
        ConfigurationHelper.resolvePlaceHolders(settingsCopy);

        applyServiceContributingIntegrators();
        applyCustomServiceContributors();

        return new CustomServiceRegistryImpl(
                autoCloseRegistry,
                bootstrapServiceRegistry,
                initiators,
                providedServices,
                settingsCopy
        );
    }

    @SuppressWarnings("deprecation")
    private void applyServiceContributingIntegrators() {
        for (Integrator integrator : bootstrapServiceRegistry.getService(IntegratorService.class).getIntegrators()) {
            if (CustomServiceContributingIntegrator.class.isInstance(integrator)) {
                CustomServiceContributingIntegrator.class.cast(integrator).prepareServices(this);
            }
        }
    }

    private void applyCustomServiceContributors() {
        final LinkedHashSet<CustomServiceContributor> serviceContributors =
                bootstrapServiceRegistry.getService(ClassLoaderService.class)
                        .loadJavaServices(CustomServiceContributor.class);

        for (CustomServiceContributor serviceContributor : serviceContributors) {
            serviceContributor.contribute(this);
        }
    }

    /**
     * Temporarily exposed since Configuration is still around and much code still uses Configuration.  This allows
     * code to configure the builder and access that to configure Configuration object (used from HEM atm).
     *
     * @return The settings map.
     * @deprecated Temporarily exposed since Configuration is still around and much code still uses Configuration.
     * This allows code to configure the builder and access that to configure Configuration object (used from HEM atm).
     */
    @Deprecated
    public Map getSettings() {
        return settings;
    }

    /**
     * Destroy a service registry.  Applications should only destroy registries they have explicitly created.
     *
     * @param serviceRegistry The registry to be closed.
     */
    public static void destroy(ServiceRegistry serviceRegistry) {
        ((StandardServiceRegistryImpl) serviceRegistry).destroy();
    }
}
