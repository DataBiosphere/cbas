package bio.terra.cbas.config;

import java.util.Properties;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** Base class for accessing database connection configuration properties. */
public class BaseDatabaseConfiguration {
  private String uri;
  private String username;
  private String password;

  // Not a property
  private PoolingDataSource<PoolableConnection> dataSource;

  public String getUri() {
    return uri;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  // NOTE: even though the setters appear unused, the Spring infrastructure uses them to populate
  // the properties.
  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  // Main use of the configuration is this pooling data source object.
  public PoolingDataSource<PoolableConnection> getDataSource() {
    // Lazy allocation of the data source
    if (dataSource == null) {
      configureDataSource();
    }
    return dataSource;
  }

  private void configureDataSource() {
    Properties props = new Properties();
    props.setProperty("user", getUsername());
    props.setProperty("password", getPassword());

    ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(getUri(), props);

    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);

    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }
}
