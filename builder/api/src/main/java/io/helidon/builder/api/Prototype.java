/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.builder.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.config.Config;
import io.helidon.common.config.ConfiguredProvider;
import io.helidon.common.config.NamedService;

/**
 * Prototype is generated from a prototype blueprint, and it is expected to be part of the public API of the module.
 * This class holds all types related to generating prototypes form a blueprint.
 */
public final class Prototype {
    private Prototype() {
    }

    /**
     * Marker interface for the prototype API, usually a generated interface.
     */
    public interface Api {
    }

    /**
     * Terminating method of this builder that creates a prototype instance.
     * If the object is a factory, it has a further method {@code build}, that produces the target runtime instance
     * created from this builder or setup object instance.
     *
     * @param <BUILDER>   type of the builder. see {@link #self()}
     * @param <PROTOTYPE> type of the prototype to be built
     * @see Prototype.Factory#build()
     */
    public interface Builder<BUILDER, PROTOTYPE> {
        /**
         * Create an instance of the {@link Prototype}. This method is available on
         * all generated builders for {@link Prototype.Blueprint}.
         *
         * @return an instance of the setup object created from this builder
         */
        PROTOTYPE buildPrototype();

        /**
         * Instance of this builder as the correct type.
         *
         * @return this instance typed to correct type
         */
        @SuppressWarnings("unchecked")
        default BUILDER self() {
            return (BUILDER) this;
        }
    }

    /**
     * Extension of {@link io.helidon.builder.api.Prototype.Builder} that supports configuration.
     * If a blueprint is marked as {@code @Configured}, build will accept configuration.
     *
     * @param <BUILDER>   type of the builder
     * @param <PROTOTYPE> type of the prototype to be built
     */
    public interface ConfiguredBuilder<BUILDER, PROTOTYPE> extends Builder<BUILDER, PROTOTYPE> {
        /**
         * Update builder from configuration.
         * Any configured option that is defined on this prototype will be checked in configuration, and if it exists,
         * it will override current value for that option on this builder.
         * Options that do not exist in the provided config will not impact current values.
         * The config instance is kept and may be used in builder interceptor, it is not available in prototype implementation.
         *
         * @param config configuration to use
         * @return updated builder instance
         */
        BUILDER config(Config config);

        /**
         * Discover services from configuration.
         *
         * @param config               configuration located at the node of the service providers
         *                             (either a list node, or object, where each child is one service)
         * @param serviceLoader        helidon service loader for the expected type
         * @param providerType         type of the service provider interface
         * @param configType           type of the configured service
         * @param allFromServiceLoader whether all services from service loader should be used, or only the ones with configured
         *                             node
         * @param <S>                  type of the expected service
         * @param <T>                  type of the configured service provider that creates instances of S
         * @return list of discovered services
         */
        default <S extends NamedService, T extends ConfiguredProvider<S>> List<S>
        discoverServices(Config config,
                         HelidonServiceLoader<T> serviceLoader,
                         Class<T> providerType,
                         Class<S> configType,
                         boolean allFromServiceLoader) {
            return ProvidedUtil.discoverServices(config, serviceLoader, providerType, configType, allFromServiceLoader);
        }
    }

    /**
     * A prototype {@link Prototype.Blueprint} may extend this interface
     * to explicitly reference the associated runtime type.
     * <p>
     * For example a {@code RetryPrototypeBlueprint} that extends a {@link Prototype.Factory}
     * of {@code Retry}, will add methods to the prototype interface and builder to build an instance of
     * {@code Retry} directly.
     * A factory method must exist either on the runtime type (such as {@code Retry}) with signature
     * {@code static Retry create(RetryPrototype)}, or on the prototype blueprint.
     *
     * @param <T> type of the runtime object (such as {@code Retry} in the description above)
     */
    public interface Factory<T> {
        /**
         * Create a new instance of the runtime type from this config object.
         *
         * @return new configured runtime instance
         */
        T build();
        /*
        This method intentionally clashes with io.helidon.common.Builder.build(), as we need to have it
        available both on the builder and on the prototype instance.
         */
    }

    /**
     * A package local type (by design) that defines getter methods and possible static factory methods
     * that form prototype information that is generated through annotation processing.
     * <p>
     * The following names are prohibited from being builder properties:
     * <ul>
     *     <li>{@code build} - builder terminating method that builds runtime type from builder,
     *                          and creates runtime type from prototype</li>
     *     <li>{@code buildPrototype} - builder terminating method that builds prototype</li>
     *     <li>{@code get} - same as {@code build}, inherited from {@link java.util.function.Supplier}</li>
     * </ul>
     */
    @Target(ElementType.TYPE)
    // note: class retention needed for cases when derived builders are inherited across modules
    @Retention(RetentionPolicy.CLASS)
    public @interface Blueprint {
        /**
         * The generated interface is public by default. We can switch it to package local
         * by setting this property to {@code false}-
         * @return whether the generated interface should be public
         */
        boolean isPublic() default true;

        /**
         * Builder can be set to be package local only. In such a case, all handling must be done
         * through other static factory methods (hides the builder from the world).
         *
         * @return whether factory methods to obtain builder should be public on prototype
         */
        boolean builderPublic() default true;

        /**
         * Method create(config) is public by default.
         *
         * @return whether factory method create(config) should be public on prototype
         */
        boolean createFromConfigPublic() default true;

        /**
         * Method create() is public by default.
         *
         * @return whether factory method create() should be public on prototype
         */
        boolean createEmptyPublic() default true;

        /**
         * Whether to use bean style setters and getters, or not (default is not).
         * If set to {@code true}, only methods starting with {@code get} would be used,
         * and all setters will start with {@code set}, except for add methods.
         *
         * @return whether to use bean style accessors, defaults to false
         */
        boolean beanStyle() default false;

        /**
         * Used to decorate the builder, right before method build is called.
         * Validations are done AFTER the interceptor is handled.
         * This class may be package local if located in the same package as blueprint.
         * The class must have accessible constructor with no parameters.
         *
         * @return decorator type
         */
        Class<? extends BuilderDecorator> decorator() default BuilderDecorator.class;
    }

    /**
     * Provides a contract by which the {@link Prototype.Blueprint}
     * annotated type can be intercepted (i.e., including decoration or
     * mutation).
     * <p>
     * The implementation class type must provide a no-arg accessible constructor available to the generated class
     * <p>
     * Note that when intercepting config, you may get {@code null} even for required types, as these may be configured
     * by your very builder interceptor.
     *
     * @param <T> the type of the bean builder to intercept
     * @see io.helidon.builder.api.Prototype.Blueprint#decorator()
     */
    @FunctionalInterface
    public interface BuilderDecorator<T> {
        /**
         * Provides the ability to intercept (i.e., including decoration or mutation) the target.
         *
         * @param target the target being intercepted
         */
        void decorate(T target);
    }

    /**
     * Adding this annotation in conjunction with the {@link Prototype.Blueprint} on a target interface
     * type or method causes the {@link #value()} be added to the generated implementation class and methods respectfully.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.CLASS)
    public @interface Annotated {

        /**
         * The annotation(s) to add to the generated methods and field names on the generated class w/ builder.
         *
         * @return The annotation(s) to add the generated method and field names
         */
        String[] value();

    }

    /**
     * This is an annotation used by Helidon code generator that marks a static method
     * as a factory method.
     * <p>
     * This annotation must be defined on any static method that should be used as a factory for runtime types from
     * prototypes, and from configuration on {@link Prototype.Blueprint}.
     * <p>
     * This annotation is generated for the following signatures:
     * <ul>
     *     <li>{@code static Prototype.Builder Prototype.builder()} - a method that returns a builder that extends
     *          {@code io.helidon.common.Builder}, that builds the prototype</li>
     *     <li>{@code static Prototype.Builder Prototype.builder(Prototype)} - a method that returns a builder populated from
     *          existing prototype instance</li>
     *     <li>{@code static Prototype create(io.helidon.common.config.Config config)} - a method that creates a new instance of
     *          prototype from configuration</li>
     *     <li>{@code static Prototype create()} - a method that creates a new instance if there are no required fields</li>
     * </ul>
     * This annotation is also used for triggering an additional round of annotation processing by the generated types, to
     * finalize
     * validation.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.CLASS)
    public @interface FactoryMethod {
    }

    /**
     * Applying this annotation to a {@link Prototype.Blueprint}-annotated interface method will cause
     * the generated class to also include additional "add*()" methods. This will only apply, however, if the method is for
     * a {@link java.util.Map}, {@link java.util.List}, or {@link java.util.Set}.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Singular {

        /**
         * The optional value specified here will determine the singular form of the method name.
         * For instance, if we take a method like this:
         * <pre>{@code
         * @Singlular("pickle")
         * List<Pickle> getPickles();
         * }</pre>
         * an additional generated method named {@code addPickle(Pickle val)} will be placed on the builder of the generated
         * class.
         * <p>This annotation only applies to getter methods that return a Map, List, or Set. If left undefined then the add
         * method
         * will use the default method name, dropping any "s" that might be present at the end of the method name (e.g.,
         * pickles -> pickle).
         *
         * @return The singular name to add
         */
        String value() default "";
    }

    /**
     * Useful for marking map properties, where the key and value must have the
     * same generic type. For example you can declare a Map of a generalized type (such as Class)
     * to an Object. This would change the singular setters to ones that expect the types to be the same.
     * <p>
     * Example:
     * For {@code Map<Class<Object>, Object>}, the following method would be generated:
     * {@code <TYPE> BUILDER put(Class<TYPE> key, TYPE value)}.
     * <p>
     * This annotation is only allowed on maps that have a key with a single type parameter,
     * and value of Object, or with a single type parameter.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface SameGeneric {
    }

    /**
     * Additional methods from this type will be added to the prototype and its builder.
     * All methods must be declared as static.
     * <p>
     * These methods can be annotated with:
     * <ul>
     *     <li>{@link Prototype.FactoryMethod} - to create a static factory method on prototype</li>
     *     <li>{@link Prototype.BuilderMethod} - to create an additional method on prototype builder, first
     *     parameter is the builder instance, may have additional parameters, must be void</li>
     *     <li>{@link Prototype.PrototypeMethod} - to be added to the prototype interface, first parameter
     *     is the prototype instance, may have additional parameters</li>
     * </ul>
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    public @interface CustomMethods {
        /**
         * Type that implements static methods to be available on the prototype.
         *
         * @return type with static methods annotated with {@link Prototype.FactoryMethod}
         */
        Class<?> value();
    }

    /**
     * Annotated static method of a custom methods type to be added to builder.
     * First parameter must be the builder type. Return type must be void.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.CLASS)
    public @interface BuilderMethod {
    }

    /**
     * Annotated static method of a custom methods type to be added to prototype interface.
     * First parameter must be the prototype instance.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.CLASS)
    public @interface PrototypeMethod {
    }

    /**
     * Add additional interfaces to implement by the prototype. Provide correct types (fully qualified) for generics.
     */
    public @interface Implement {
        /**
         * Interfaces to implement, such as {@code java.lang.Comparable<io.helidon.common.types.TypeName>}.
         *
         * @return interfaces to implement
         */
        String[] value();
    }

    /**
     * Mark a getter method as redundant - not important for {@code equals}, {@code hashcode}, and/or {@code toString}.
     * The generated prototype will ignore the fields for these methods in equals and hashcode methods.
     * All other fields will be included.
     * <p>
     * In case both properties are set to false, it is af this annotation is not present.
     */
    @Target(ElementType.METHOD)
    // note: class retention needed for cases when derived builders are inherited across modules
    @Retention(RetentionPolicy.CLASS)
    public @interface Redundant {
        /**
         * Set to {@code false} to mark this NOT redundant for equals and hashcode.
         *
         * @return whether this should be ignored for equals and hashcode
         */
        boolean equality() default true;

        /**
         * Set to {@code false} to mark this NOT redundant for toString.
         *
         * @return whether this should be ignored for toString
         */
        boolean stringValue() default true;
    }

    /**
     * Mark a getter method as confidential - not suitable to be used in clear text in {@code toString} method.
     * The field will be mentioned (whether it has a value or not), but its value will not be visible.
     */
    @Target(ElementType.METHOD)
    // note: class retention needed for cases when derived builders are inherited across modules
    @Retention(RetentionPolicy.CLASS)
    public @interface Confidential {
    }
}
