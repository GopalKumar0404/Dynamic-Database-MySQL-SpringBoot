package com.gopal.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import com.gopal.payload.database.DatabaseDetails;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class DynamicEntityService {

	private DataSource dataSource;
	private Map<String, Class<?>> entityClassMap = new HashMap<>();
	private Map<String, Object> repositoryMap = new HashMap<>();
	@Autowired
	private DataSource defaultDataSource;

	@PostConstruct
	public void initialize() {
		this.dataSource = defaultDataSource;
	}

	public void setDataSource(DatabaseDetails details) throws SQLException, IOException {
		this.dataSource = createDataSource(details);
		try {
			clearOldEntitiesAndRepositories();
			generateEntitiesAndRepositories();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException
				| IOException e) {
			e.printStackTrace();
		}
	}

	@Primary
	private DataSource createDataSource(DatabaseDetails details) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(details.getJdbcUrl());
		config.setUsername(details.getUsername());
		config.setPassword(details.getPassword());
		config.setDriverClassName(details.getDriverClassName());
		return new HikariDataSource(config);
	}

	@PreDestroy
//	 @PostConstruct
	private void clearOldEntitiesAndRepositories() {
		System.out.println("Deleting all the dynamic created java and class files.....");
		// deleting of java files
		String basePath = Paths.get(System.getProperty("user.dir"), "src", "main", "java", "com", "gopal").toString();
		File entityPackage = new File(basePath, "entity");
		File repositoryPackage = new File(basePath, "repository");

		deleteDirectoryContents(entityPackage);
		deleteDirectoryContents(repositoryPackage);

		// deleting of class files
		String baseClassPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "com", "gopal")
				.toString();
		File classEntityPackage = new File(baseClassPath, "entity");
		File classRepositoryPackage = new File(baseClassPath, "repository");

		deleteDirectoryContents(classEntityPackage);
		deleteDirectoryContents(classRepositoryPackage);

		System.out.println("Deletion completed shutting down the applicaiton.....");
	}

	private void deleteDirectoryContents(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			for (File file : directory.listFiles()) {
				if (file.isFile()) {
					file.delete();
				}
			}
		}
	}

	private void generateEntitiesAndRepositories()
			throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			String catalog = connection.getCatalog();
			String schema = connection.getSchema();
			ResultSet tables = metaData.getTables(catalog, schema, "%", new String[] { "TABLE" });

			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				String primaryKeyType = getPrimaryKeyType(metaData, catalog, schema, tableName);

				String entityClass = generateEntityClass(tableName, metaData, catalog, schema, primaryKeyType);
				writeJavaFile(tableName, entityClass, "entity");
				compileJavaFile(tableName, "entity");

				Class<?> clazz = compileAndLoadClass(tableName, "entity");
				entityClassMap.put(tableName, clazz);

				String repositoryClass = generateRepositoryClass(tableName, metaData, catalog, schema, primaryKeyType);
				writeJavaFile(tableName, repositoryClass, "repository");
				compileJavaFile(tableName, "repository");

				Object repository = compileAndLoadRepository(tableName, clazz, primaryKeyType);
				repositoryMap.put(tableName, repository);
			}
		}
	}

	private String getPrimaryKeyType(DatabaseMetaData metaData, String catalog, String schema, String tableName)
			throws SQLException {
		ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName);
		if (primaryKeys.next()) {
			String pkColumn = primaryKeys.getString("COLUMN_NAME");
			ResultSet columns = metaData.getColumns(catalog, schema, tableName, pkColumn);
			if (columns.next()) {
				String columnType = columns.getString("TYPE_NAME");
				return mapSqlTypeToJavaType(columnType);
			}
		}
		return "Long"; // Default to Long if primary key is not found
	}

	private String generateEntityClass(String tableName, DatabaseMetaData metaData, String catalog, String schema,
			String primaryKeyType) throws SQLException {
		ResultSet columns = metaData.getColumns(catalog, schema, tableName, "%");
		ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName);
		String primaryKeyColumn = null;

		if (primaryKeys.next()) {
			primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
		}

		StringBuilder entityClass = new StringBuilder("package com.gopal.entity;\n\n");
		entityClass.append("import jakarta.persistence.*;\n");
		entityClass.append("import lombok.Data;\n");
		entityClass.append("import lombok.NoArgsConstructor;\n");
		entityClass.append("import lombok.AllArgsConstructor;\n\n");
		entityClass.append("@Data\n");
		entityClass.append("@NoArgsConstructor\n");
		entityClass.append("@AllArgsConstructor\n");
		entityClass.append("@Entity\n");
		entityClass.append("@Table(name = \"").append(tableName).append("\")\n");
		entityClass.append("public class ").append(toCamelCase(tableName, true)).append(" {\n");

		while (columns.next()) {
			String columnName = columns.getString("COLUMN_NAME");
			String columnType = columns.getString("TYPE_NAME");
			entityClass.append("    @Column(name = \"").append(columnName).append("\")\n");

			if (columnName.equals(primaryKeyColumn)) {
				entityClass.append("    @Id\n");
				entityClass.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
				entityClass.append("    private ").append(primaryKeyType).append(" ")
						.append(toCamelCase(columnName, false)).append(";\n\n");
			} else {
				entityClass.append("    private ").append(mapSqlTypeToJavaType(columnType)).append(" ")
						.append(toCamelCase(columnName, false)).append(";\n\n");
			}
		}

		entityClass.append("    // Getters and Setters are handled by Lombok\n");
		entityClass.append("}\n");
		return entityClass.toString();
	}

	private String generateRepositoryClass(String tableName, DatabaseMetaData metaData, String catalog, String schema,
			String primaryKeyType) throws SQLException {
		String className = toCamelCase(tableName, true);
		ResultSet columns = metaData.getColumns(catalog, schema, tableName, "%");
		String repositoryClass = "package com.gopal.repository;\n\n";
		repositoryClass += "import org.springframework.data.jpa.repository.JpaRepository;\n";
		repositoryClass += "import org.springframework.stereotype.Repository;\n";
		repositoryClass += "import com.gopal.entity." + className + ";\n\n";
		repositoryClass += ("import java.util.Optional;\n");
		repositoryClass += ("import java.util.List;\n\n");
		repositoryClass += "@Repository\n";
		repositoryClass += "public interface " + className + "Repository extends JpaRepository<" + className + ", "
				+ primaryKeyType + "> {\n";
//		repositoryClass += "}\n";

		StringBuilder repositoryContent = new StringBuilder(repositoryClass);
		while (columns.next()) {
			String columnName = columns.getString("COLUMN_NAME");
			String javaType = mapSqlTypeToJavaType(columns.getString("TYPE_NAME"));
			String camelCaseColumnName = toCamelCase(columnName, true);
			repositoryContent.append("    Optional<List<").append(className).append(">> findBy")
					.append(camelCaseColumnName).append("(").append(javaType).append(" ").append(columnName)
					.append(");\n");
		}

		repositoryContent.append("}\n");
		return repositoryContent.toString();
	}

	private void writeJavaFile(String tableName, String classContent, String type) throws IOException {
		String className = toCamelCase(tableName, true);
		String packageName = type.equals("entity") ? "entity" : "repository";
		String fileName = Paths.get(System.getProperty("user.dir"), "src", "main", "java", "com", "gopal", packageName,
				className + (type.equals("entity") ? ".java" : "Repository.java")).toString();
		File file = new File(fileName);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(classContent);
		}
	}

	private void compileJavaFile(String tableName, String type) throws IOException {
		String className = toCamelCase(tableName, true);
		String packageName = type.equals("entity") ? "entity" : "repository";
		String basePath = Paths.get(System.getProperty("user.dir"), "src", "main", "java", "com", "gopal", packageName)
				.toString();
		String fileName = Paths.get(basePath, className + (type.equals("entity") ? ".java" : "Repository.java"))
				.toString();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int compilationResult = compiler.run(null, null, null, fileName);

		if (compilationResult != 0) {
			System.out.println("Compilation failed.");
			return;
		}

		// Compile the .java file to .class file in target directory
		String targetPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "com", "gopal", packageName)
				.toString();
		File targetDir = new File(targetPath);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}

		String classFileName = toCamelCase(tableName, true) + (type.equals("entity") ? ".class" : "Repository.class");
		File compiledClassFile = new File(basePath, classFileName);
		File targetClassFile = new File(targetPath, classFileName);
		if (compiledClassFile.exists()) {
			compiledClassFile.renameTo(targetClassFile);
		}
	}

	private Class<?> compileAndLoadClass(String tableName, String type) throws ClassNotFoundException {
		Class<?> dynamicClass = null;
		String className = toCamelCase(tableName, true) + (type.equals("entity") ? "" : "Repository");
		String packageName = type.equals("entity") ? "entity" : "repository";
		String fullClassName = "com.gopal." + packageName + "." + className;
		String targetPath = Paths.get(System.getProperty("user.dir"), "target", "classes", "com", "gopal", packageName)
				.toString();

		try (URLClassLoader classLoader = URLClassLoader
				.newInstance(new URL[] { new File(targetPath).toURI().toURL() })) {
			System.out.println(new File(targetPath).toURI().toURL().toString() + " target file path of classes");
			System.out.println("Class Name to Load : " + fullClassName);
			dynamicClass = classLoader.loadClass(fullClassName);
			entityClassMap.put(toCamelCase(tableName, true), dynamicClass);
		} catch (ClassNotFoundException | IllegalArgumentException | SecurityException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error in loading the class file");
			e.printStackTrace();
		}
		return Class.forName(fullClassName);
	}

	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private BeanFactory beanFactory;

	private Object compileAndLoadRepository(String tableName, Class<?> entityClass, String primaryKeyType)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Class<?> repositoryClass = compileAndLoadClass(tableName, "repository");
		
		String className = toCamelCase(tableName, true) + "Repository";
		String fullClassName = "com.gopal." + "repository" + "." + className;
		scanAndRegisterBeans("com.gopal");
		if (repositoryClass == null) {
			System.out.println("Repository Class is null !!");
		}
		try {
			JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager);
			return  factory.getRepository(repositoryClass);
			
//			return Class.forName(fullClassName).newInstance();
		} catch (Exception e) {
			System.out.println("Repository Object creation failed!!!!");
			e.printStackTrace();
		}
		return null;
	}

	private String toCamelCase(String s, boolean capitalizeFirstLetter) {
		StringBuilder result = new StringBuilder();
		boolean capitalize = capitalizeFirstLetter;
		for (char c : s.toCharArray()) {
			if (c == '_') {
				capitalize = true;
			} else if (capitalize) {
				result.append(Character.toUpperCase(c));
				capitalize = false;

			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	private String mapSqlTypeToJavaType(String sqlType) {
		Map<String, String> typeMap = new HashMap<>();
		typeMap.put("VARCHAR", "String");
		typeMap.put("CHAR", "String");
		typeMap.put("TEXT", "String");
		typeMap.put("INT", "Integer");
		typeMap.put("BIGINT", "Long");
		typeMap.put("FLOAT", "Float");
		typeMap.put("DOUBLE", "Double");
		typeMap.put("DATE", "Date");
		typeMap.put("TIMESTAMP", "Timestamp");
		typeMap.put("BOOLEAN", "Boolean");
		// Add more mappings as needed

		return typeMap.getOrDefault(sqlType, "String");
	}

	public Object findAll(String tableName) throws Exception {
		Object repository = repositoryMap.get(tableName);
		Method method = repository.getClass().getMethod("findAll");
		return method.invoke(repository);
	}

	public Object findById(String tableName, Object id) throws Exception {
		Object repository = repositoryMap.get(tableName);
		Method method = repository.getClass().getMethod("findById", id.getClass());
		return method.invoke(repository, id);
	}

	public Object save(String tableName, Object entity) throws Exception {
		Object repository = repositoryMap.get(tableName);
		Method method = repository.getClass().getMethod("save", entityClassMap.get(tableName));
		return method.invoke(repository, entity);
	}

	public void deleteById(String tableName, Object id) throws Exception {
		Object repository = repositoryMap.get(tableName);
		Method method = repository.getClass().getMethod("deleteById", id.getClass());
		method.invoke(repository, id);
	}
	
	public void scanAndRegisterBeans(String... BASE_PACKAGES) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Repository.class)); // Adjust as needed

        for (String basePackage : BASE_PACKAGES) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition beanDefinition : beanDefinitions) {
                registerBean(beanDefinition);
            }
        }
    }

    private void registerBean(BeanDefinition beanDefinition) {
        try {
            Class<?> beanClass = ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), ClassUtils.getDefaultClassLoader());
            // You can do further validation here if needed
            // For example, check if the class has specific annotations or implements certain interfaces
            // Then dynamically register the bean with the application context
            // applicationContext.registerBean(beanClass);
            System.out.println("Found repository class: " + beanClass.getName());
        } catch (Throwable e) {
            // Handle any errors or exceptions
            e.printStackTrace();
        }
    }
}
