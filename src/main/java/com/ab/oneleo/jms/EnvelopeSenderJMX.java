package com.ab.oneleo.jms;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ab.ah.scad.acl.batchmanagement.dao.BatchStrategyDaoSupport;
import com.ab.oneleo.interoperability.util.JAXBHelper;
import com.ab.tsf.esb.enterprise.message.Body;
import com.ab.tsf.esb.enterprise.message.Envelope;
import com.ab.tsf.esb.enterprise.message.Header;
import com.ab.tsf.esb.enterprise.message.MessageDataFormatEnum;
import com.ab.tsf.esb.enterprise.message.StandardHeader;
import com.mchange.v2.c3p0.ComboPooledDataSource;



/**
 * @author mdeluca
 *
 */

public class EnvelopeSenderJMX {

	public static final Logger logger = LoggerFactory.getLogger(EnvelopeSenderJMX.class);

	private static String SEND_COMMAND="send";
	private static String RECEIVE_COMMAND="receive";
	private static String CLEAR_COMMAND="clear";
	
	protected Properties envelopeSenderProperties;
	protected ApplicationContext ctx;

	private static String ENVIRONMENT_DB = "environment_db";
	private static String ENVIRONMENT_STAGING = "environment_staging";
	private static String TRANSACTION_ID_LIST_FILE_PATH = "transactionIDListFilePath";
	private static String SQL_FILE_NAME = "sqlFileName";
	private static String INTERFACE_TYPE = "interfaceType";
	private static String TRACKING = "tracking";
	private static String STAGING_TABLE = "stagingTable";
	private static String TRANSACTION_ID = "transactionID";

	// HEADER ENVELOPE
	private static String PATTERN_MESSAGE_ID = "patternMessageID";
	private static String START_FROM_MESSAGE_ID = "startFromMessageID";
	private static String PREFIX_MESSAGE_ID = "prefixMessageID";
	private static String INTERFACE_OPERATION = "interfaceOperation";
	private static String API_ID = "API_ID";
	private static String COUNTRY_ID = "countryID";
	private static String SITE_ID = "siteID";
	private static String STORE_ID = "storeID";

	private static String FILE = "file";
	private static String STAGING = "staging";
	private static String INPUT_FILE_PATH = "inputFilePath";
	
	private static String MANAGEMENT_URL="management.url";
	private static String MANAGEMENT_HOST="management.host";
	private static String MANAGEMENT_PORT="management.port";
	private static String MANAGEMENT_REMOTEOPERATION="management.remoteOperation";
	

	protected BatchStrategyDaoSupport batchDaoFromStaging;
	protected BatchStrategyDaoSupport batchDao;
	protected JAXBHelper jaxbMarshaller;
	
	public EnvelopeSenderJMX() throws Exception {
		this.init();
	}

	/**
	 * @throws Exception
	 *************************/
	public static void main(String args[]) throws Exception {

		EnvelopeSenderJMX main = new EnvelopeSenderJMX();
		
		try {
			main.sendMessage();
		}catch (Exception e) {
			logger.error("ERROR: ", e);
		}
		
	}
	
	

	public void sendMessage() throws Exception {
		List<String> listEnvelope = getListEnvelope();
		sendMessage(listEnvelope);
	}
	
	
	public void sendMessage(List<String> listEnvelope){
		
		String apiID = envelopeSenderProperties.getProperty(envelopeSenderProperties.getProperty(INTERFACE_OPERATION)+"."+API_ID);
		String remoteoperation = envelopeSenderProperties.getProperty(MANAGEMENT_REMOTEOPERATION);

		String management_url = envelopeSenderProperties.getProperty(MANAGEMENT_URL);
		String management_host = envelopeSenderProperties.getProperty(MANAGEMENT_HOST);
		String management_port = envelopeSenderProperties.getProperty(MANAGEMENT_PORT);
		
		String url = management_url+"://"+management_host+":"+management_port;
		
		url=System.getProperty("jmx.service.url",url);
		JMXConnector connector = null;
		try {
			JMXServiceURL target = new JMXServiceURL(url);
			
			Map<String,Object> env = new HashMap<String, Object>();
			/*	
			// Authenticate as C. User and password "mysecret"
			env.put(Context.SECURITY_AUTHENTICATION, "DIGEST-MD5");
			//env.put(Context.SECURITY_PRINCIPAL, "dn:cn=oneleonardoOperations, ou=NewHires, o=JNDITutorial");
			env.put(Context.SECURITY_PRINCIPAL, "dn:cn=oneleonardoOperations");
			env.put(Context.SECURITY_CREDENTIALS, "asm01");
			// Request privacy protection
			env.put("javax.security.sasl.qop", "auth-conf");
			// Request medium-strength cryptographic protection
			env.put("javax.security.sasl.strength", "medium");
			 */

			//			env.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());

			/*
			env.put(Context.SECURITY_PROTOCOL, "ssl");
			env.put(Context.SECURITY_AUTHENTICATION, "DIGEST-MD5");
			env.put(Context.SECURITY_PRINCIPAL, "u:oneleonardoOperations");
			env.put(Context.SECURITY_CREDENTIALS, "asm01");
			*/
			/*
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] pass = md.digest("asm01".getBytes());
			
			String[] creds = {"oneleonardoOperations", pass.toString()};
			//String[] creds = {"john", null};
	        env.put(JMXConnector.CREDENTIALS, creds);
	        */
	        
			/*
	        final Hashtable jndiProperties = new Hashtable();
	        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
	        jndiProperties.put(Context.SECURITY_PRINCIPAL, "oneleonardoOperations");
	        jndiProperties.put(Context.SECURITY_CREDENTIALS, "asm01");

	        InitialContext aJNDI = new InitialContext(jndiProperties);
			*/
			
			String[] creds = {"monitorRole", "asm01"};
			env.put(JMXConnector.CREDENTIALS, creds);
			
			//env.put("jmx.remote.protocol.provider.pkgs", "org.jboss.remotingjmx");
	        connector = JMXConnectorFactory.connect(target,env);
	        //connector = JMXConnectorFactory.connect(target);
	        
	        MBeanServerConnection remote = connector.getMBeanServerConnection();
	
	        ObjectName bean = new ObjectName("interoperability-jms:name=interoperabilityEmulator,type=InteroperabilityEmulator");
	        //MBeanInfo info = remote.getMBeanInfo(bean);
	        
	        String  opSig[] = {String.class.getName(),String.class.getName(), Boolean.class.getName()};
	        
	        for (String envelope : listEnvelope) {
	        	
	        	//envelope = new String(envelope.getBytes(),"UTF-8");
	        	Object  opParams[] = {apiID, envelope, false};
	        	Object ris = remote.invoke(bean, remoteoperation, opParams, opSig);
	        	logger.info("Invoker result: "+ris);
	        }
		} catch (Exception e) {
			logger.error("ERROR: ",e);
		} finally {
			if (connector != null) {
				try {
					connector.close();
				} catch (IOException e) {
					logger.error("ERROR: ",e);
				}
			}
		}
	}
	
	
	
	
	

	private List<String> getListEnvelope() throws Exception {

		String interfaceType = envelopeSenderProperties.getProperty(INTERFACE_TYPE);
		List<String> listEnvelope = null;

		if (interfaceType == null) {
			throw new Exception("NO INTERFACE TYPE SELECTED!!!");

		} else if (interfaceType.equalsIgnoreCase(TRACKING)) {
			listEnvelope = getListEnvelopeFromTracking();
			//listEnvelope = removeDuplicated(listEnvelope);

		} else if (interfaceType.equalsIgnoreCase(FILE)) {
			listEnvelope = getListEnvelopeFromFile();
			//listEnvelope = removeDuplicated(listEnvelope);

		} else if (interfaceType.equalsIgnoreCase(STAGING)) {
			listEnvelope = getListEnvelopeFromStaging();
			// listEnvelope = removeDuplicated(listEnvelope);
		}
		return listEnvelope;
	}

	private List<String> getListEnvelopeFromStaging() throws Exception {

		List<String> listEnvelope = new ArrayList<String>();

		String stagingTable = envelopeSenderProperties.getProperty(STAGING_TABLE);
		String transactionId = envelopeSenderProperties.getProperty(TRANSACTION_ID);

		if (stagingTable == null || stagingTable.isEmpty())
			throw new Exception("PROPERTY STAGING TABLE NOT DEFINED");

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ENTITY FROM " + stagingTable + " WHERE 1=1 ");

		if (transactionId != null && !transactionId.isEmpty())
			sql.append(" AND TRANSACTION_ID = \'" + transactionId + "\'");

		List<Map<String, Object>> rows = batchDaoFromStaging.queryForList(sql
				.toString());
		int i = 0;
		for (Map row : rows) {
			try {
				String entityXml = (String) row.get("ENTITY");
				Object entity = jaxbMarshaller.unmarshal(entityXml);
				Envelope envelope = buildEnvelope(entity, i);
				Body body = new Body();
				body.setAny(entity);
				envelope.setBody(body);
				listEnvelope.add(jaxbMarshaller.marshal(envelope));
				i++;
			} catch (Exception e) {
				logger.error("Errore riga=" + row, e);
			}

		}

		return listEnvelope;
	}

	private Envelope buildEnvelope(Object entity, int i) {

		Envelope envelope = new Envelope();
		Date now = new Date();

		Header header = new Header();
		StandardHeader standardHeader = new StandardHeader();
		standardHeader.setSourceID("ONELEO");
		standardHeader.setSourceApplicationID("ONELEO");

		String patternMessageID = envelopeSenderProperties.getProperty(PATTERN_MESSAGE_ID);
		String startFromMessageID = envelopeSenderProperties.getProperty(START_FROM_MESSAGE_ID);
		String prefixMessageID = envelopeSenderProperties.getProperty(PREFIX_MESSAGE_ID);
		
		if (i > 0 && prefixMessageID != null && patternMessageID != null && startFromMessageID != null) {
			String messageID = String.format(patternMessageID, prefixMessageID,	i + Integer.parseInt(startFromMessageID));
			standardHeader.setMessageID(messageID);
			standardHeader.setMessageCorrelationID(messageID);
		} else {
			standardHeader.setMessageID("EBS-" + now.getTime());
			standardHeader.setMessageCorrelationID("EBS-" + now.getTime());
		}

		standardHeader.setMessageCreationDatetime(now);
		standardHeader.setMessageDataFormat(MessageDataFormatEnum.XML);
		standardHeader.setMessageResultCode(0);
		standardHeader.setInterfaceOperation(envelopeSenderProperties.getProperty(INTERFACE_OPERATION));
		standardHeader.setInterfaceID(envelopeSenderProperties.getProperty(API_ID));
		standardHeader.setCountryID(envelopeSenderProperties.getProperty(COUNTRY_ID));
		standardHeader.setSiteID(envelopeSenderProperties.getProperty(SITE_ID));
		standardHeader.setCompanyID("Alliance");
		standardHeader.setStoreID(envelopeSenderProperties.getProperty(STORE_ID));

		standardHeader.setEntityID(buildBusinessKey(entity));

		header.setStandardHeader(standardHeader);
		envelope.setHeader(header);

		return envelope;

	}

	private Object buildBusinessKey(Object entity) {
//		if (entity instanceof SalesOrder) {
//
//			SalesOrder salesOrder = (SalesOrder) entity;
//			SalesOrderBusinessKey salesOrderBusinessKey = new SalesOrderBusinessKey();
//			salesOrderBusinessKey.setCode(salesOrder.getCode());
//			salesOrderBusinessKey.setOrderDate(salesOrder.getOrderDate());
//
//			return salesOrderBusinessKey;
//		}

		return null;
	}

	private List<String> getListEnvelopeFromTracking() throws IOException {
		List<String> listEnvelope = new ArrayList<String>();

		List<String> transactionIDList = getTransactionIDList();

		String fileName = envelopeSenderProperties.getProperty(SQL_FILE_NAME);

		String path = new File("").getAbsolutePath() + fileName;
		File file = new File(path);

		FileReader fReader = new FileReader(file);

		BufferedReader br = new BufferedReader(fReader);
		StringBuilder sql = new StringBuilder();
		String line = "";
		while ((line = br.readLine()) != null) {
			sql.append(line);
		}
		fReader.close();

		if (transactionIDList != null && !transactionIDList.isEmpty()) {
			sql.append(" AND TRANSACTION_ID IN (");
			for (int i = 0; i < transactionIDList.size(); i++) {
				sql.append("\'" + transactionIDList.get(i) + "\'");
				if (i < transactionIDList.size() - 1)
					sql.append(",\n");
			}
			sql.append(")");
		}

		List<Map<String, Object>> rows = batchDao.queryForList(sql.toString());
		for (Map row : rows) {
			try {
				String envelope = (String) row.get("PAYLOAD");
				listEnvelope.add(envelope);
			} catch (Exception e) {
				logger.error("Errore riga=" + row, e);
			}
		}

		return listEnvelope;

	}

	private List<String> getTransactionIDList() throws IOException {

		List<String> transactionIdList = new ArrayList<String>();

		String transactionIdListFile = envelopeSenderProperties.getProperty(TRANSACTION_ID_LIST_FILE_PATH);

		String path = new File("").getAbsolutePath() + transactionIdListFile;
		File file = new File(path);

		FileReader fReader = new FileReader(file);

		BufferedReader br = new BufferedReader(fReader);
		String line = "";
		while ((line = br.readLine()) != null) {
			transactionIdList.add(line);
		}
		fReader.close();

		return transactionIdList;
	}

	private List<String> getListEnvelopeFromFile() throws IOException {

		List<String> listEnvelope = new ArrayList<String>();

		// System.out.println(envelopeSenderProperties.getProperty(INPUT_FILE_PATH));
		String inputFilePath = envelopeSenderProperties.getProperty(INPUT_FILE_PATH);
		String pathFolder = new File("").getAbsolutePath() + inputFilePath;
		File inputFile = new File(pathFolder);

		if (inputFile.isDirectory()) {
			for (final File fileEntry : inputFile.listFiles()) {
				// System.out.println(fileEntry.getName());
				if (fileEntry.getName().toUpperCase().endsWith(".XML")) {
					FileReader fReader = new FileReader(fileEntry);

					BufferedReader br = new BufferedReader(fReader);
					StringBuilder xml = new StringBuilder();
					String line = "";
					while ((line = br.readLine()) != null) {
						xml.append(line);
					}
					fReader.close();
					br.close();
					listEnvelope.add(xml.toString());
				}
			}
		} else if (inputFile.isFile()) {
			FileReader fReader = new FileReader(inputFile);

			BufferedReader br = new BufferedReader(fReader);
			StringBuilder xml = new StringBuilder();
			String line = "";
			while ((line = br.readLine()) != null) {
				xml.append(line);
			}
			fReader.close();
			br.close();
			listEnvelope.add(xml.toString());
			
		}

		return listEnvelope;
	}


	public void init() throws PropertyVetoException {

		String[] contextFile = loadContext();
		ctx = new ClassPathXmlApplicationContext(contextFile);

		envelopeSenderProperties = ctx.getBean("envelopeSenderProperties",Properties.class);
		
		initDB();

		jaxbMarshaller = ctx.getBean("integrationJaxbHelper", JAXBHelper.class);
		batchDaoFromStaging = ctx.getBean("batchDaoFromStaging",BatchStrategyDaoSupport.class);
		batchDao = ctx.getBean("batchDao", BatchStrategyDaoSupport.class);
		

	}
	
	private void initDB() throws PropertyVetoException {
		
		Properties dataSourceProperties  = ctx.getBean("dataSourceProperties",Properties.class);
		
		//INIT DATA SOURCE FUNCTIONAL
		String environmentDB = envelopeSenderProperties.getProperty("environment_db");
		ComboPooledDataSource dataSource= ctx.getBean("dataSource", ComboPooledDataSource.class);
		dataSource.setDriverClass(dataSourceProperties.getProperty("jdbc.driverClassName"));
		dataSource.setJdbcUrl(dataSourceProperties.getProperty("jdbc.url."+environmentDB));
		dataSource.setUser(dataSourceProperties.getProperty("jdbc.username."+environmentDB));
		dataSource.setPassword(dataSourceProperties.getProperty("jdbc.password."+environmentDB));
		
		//INIT DATA SOURCE STAGING
		String environmentStagingDB = envelopeSenderProperties.getProperty("environment_staging");
		ComboPooledDataSource dataSourceStaging= ctx.getBean("dataSourceStaging", ComboPooledDataSource.class);

		dataSourceStaging.setDriverClass(dataSourceProperties.getProperty("jdbc.driverClassName"));
		dataSourceStaging.setJdbcUrl(dataSourceProperties.getProperty("jdbc.url."+environmentStagingDB));
		dataSourceStaging.setUser(dataSourceProperties.getProperty("jdbc.username."+environmentStagingDB));
		dataSourceStaging.setPassword(dataSourceProperties.getProperty("jdbc.password."+environmentStagingDB));

		
		
	}

	public String[] loadContext() {
			
			String[] contextFile = {"classpath:config/mocked-beans-config.xml",
								    "classpath:config/batch-datasource.xml",
								    "classpath:config/batchmanagement-dao-config.xml",
								    "classpath:config/envelopeSenderJMX-config.xml" };
			return contextFile;
	}
	

	private List<String> removeDuplicated(List<String> listEnvelope) {

		List<String> listEnvelopeFiltered = null;

		// jaxbMarshaller.setClassesToBeBound(Envelope.class);

		Map<String, String> mapEnvelopeFiltered = new HashMap<String, String>();
		if (listEnvelope != null) {
			listEnvelopeFiltered = new ArrayList<String>();
			for (String envelopeXml : listEnvelope) {
				Envelope envelope = (Envelope) jaxbMarshaller.unmarshal(envelopeXml);
				// Object bk =
				// envelope.getHeader().getStandardHeader().getEntityID();
				/*
				InterWarehouseTransferOrder interWarehouseTransferOrder = (InterWarehouseTransferOrder) envelope.getBody().getAny();
				if (mapEnvelopeFiltered.containsKey(interWarehouseTransferOrder.getCode())) {
					logger.info("#### interWarehouseTransferOrder " + interWarehouseTransferOrder.getCode()
							+ " already exist!!!");
				} else {
					mapEnvelopeFiltered.put(interWarehouseTransferOrder.getCode(), envelopeXml);
				}
				*/
			}
		}

		listEnvelopeFiltered.addAll(mapEnvelopeFiltered.values());

		return listEnvelopeFiltered;
	}

	
}
