package no.catenda.docs.c4;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an infrastructure adapter as connecting to an external system for C4 documentation diagram
 * generation. Place this on classes that implement domain ports (e.g. repositories, external
 * service clients).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SuppressWarnings("unused")
public @interface C4ExternalSystemAdapter {

  /**
   * Name of the external system as it should appear on the C4 diagram.
   */
  String externalSystemName() default "";

  /**
   * Short description of the external system.
   */
  String description() default "";

  /**
   * Description of the relationship from the main system to this external system.
   */
  String relationship() default "";

  /**
   * Technology/protocol used (e.g. "JDBC", "HTTP/REST").
   */
  String technology() default Technology.CUSTOM;

  class Technology {
    public final static String JDBC = "JDBC";
    public final static String HTTP_REST = "HTTP/REST";
    public final static String TCP_IP = "TCP/IP";
    public final static String CUSTOM = "CUSTOM";
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for HTTP/REST external systems.
   * Pre-sets {@code technology} to {@link Technology#HTTP_REST}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter(technology = Technology.HTTP_REST)
  @SuppressWarnings("unused")
  @interface RestApi {
    String externalSystemName();

    String description();

    String relationship();
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for relational database external systems.
   * Pre-sets {@code technology} to {@link Technology#JDBC}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter
  @SuppressWarnings("unused")
  @interface RelationalStore {
    String externalSystemName();

    String description();

    String relationship();

    String technology() default Technology.JDBC;
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for Document Store external systems.
   * Pre-sets {@code technology} to {@link Technology#CUSTOM}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter
  @SuppressWarnings("unused")
  @interface DocumentStore {
    String externalSystemName();

    String description();

    String relationship();

    String technology() default Technology.CUSTOM;
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for Blob Storage external systems.
   * Pre-sets {@code technology} to {@link Technology#CUSTOM}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter
  @SuppressWarnings("unused")
  @interface BlobStorage {
    String externalSystemName();

    String description();

    String relationship();

    String technology() default Technology.CUSTOM;
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for Cache external systems.
   * Pre-sets {@code technology} to {@link Technology#CUSTOM}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter
  @SuppressWarnings("unused")
  @interface Cache {
    String externalSystemName();

    String description();

    String relationship();

    String technology() default Technology.CUSTOM;
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for Search Index external systems.
   * Pre-sets {@code technology} to {@link Technology#CUSTOM}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter
  @SuppressWarnings("unused")
  @interface SearchIndex {
    String externalSystemName();

    String description();

    String relationship();

    String technology() default Technology.CUSTOM;
  }

  /**
   * Specialized {@link C4ExternalSystemAdapter} for Kafka external systems.
   * Pre-sets {@code technology} to {@link Technology#CUSTOM}.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @C4ExternalSystemAdapter
  @SuppressWarnings("unused")
  @interface Kafka {
    String externalSystemName();

    String description();

    String relationship();

    String technology() default Technology.CUSTOM;
  }
}
