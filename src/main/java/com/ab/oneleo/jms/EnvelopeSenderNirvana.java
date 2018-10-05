package com.ab.oneleo.jms;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.pcbsys.nirvana.nJMS.DestinationImpl;
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
import com.pcbsys.nirvana.nJMS.TextMessageImpl;



public class EnvelopeSenderNirvana {

	public final Logger logger = LoggerFactory.getLogger(EnvelopeSenderNirvana.class);

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
	private static String INTERFACE_ID = "interfaceID";
	private static String COUNTRY_ID = "countryID";
	private static String SITE_ID = "siteID";
	private static String STORE_ID = "storeID";

	private static String FILE = "file";
	private static String STAGING = "staging";
	private static String INPUT_FILE_PATH = "inputFilePath";

	private static String INITIAL_CONTEXT_FACTORY = "initialContextFactory";
	private static String URL_PKG_PREFIXES = "urlPkgPrefixes";
	private static String PROVIDER_URL = "providerUrl";
	private static String OUTBOUND_QUEUE = "outboundQueue";
	private static String INBOUND_QUEUE = "inboundQueue";
	private static String CONNECTION_FACTORY = "connectionFactory";

	protected BatchStrategyDaoSupport batchDaoFromStaging;
	protected BatchStrategyDaoSupport batchDao;
	protected JAXBHelper jaxbMarshaller;

	public EnvelopeSenderNirvana() throws Exception {
		this.init();
	}

	/**
	 * @throws Exception
	 *************************/
	public static void main(String args[]) throws Exception {

		EnvelopeSenderNirvana main = new EnvelopeSenderNirvana();

		if (args != null && args.length>0) {
			if(SEND_COMMAND.equalsIgnoreCase(args[0])) {
				main.sendMessage();
			} else if(RECEIVE_COMMAND.equalsIgnoreCase(args[0])) {
				main.receiveMessage();
			} else if(CLEAR_COMMAND.equalsIgnoreCase(args[0])) {
				main.clearQueue();
			}
		} else {
			throw new Exception("No arguments in input!! Inputs are:\n"+
					SEND_COMMAND+"\n"+
					RECEIVE_COMMAND+"\n"+
					CLEAR_COMMAND);
		}
		
		//main.sendMessage();
		//main.receiveMessage();
		//main.clearQueue();
		
		
	}
	
	

	public void sendMessage() throws Exception {
    Map<String, String> listEnvelope = getListEnvelope();
		sendMessage(listEnvelope);
	}
	
	
	public void sendMessage(Map<String, String> listEnvelope) throws Exception {
		
		String outboundQueue = envelopeSenderProperties.getProperty(envelopeSenderProperties.getProperty(INTERFACE_OPERATION)+"."+OUTBOUND_QUEUE);

		Context ic = null;
		ConnectionFactory cf = null;
		Connection connection = null;

		try {
			ic = getJMSContext();

			cf = (ConnectionFactory) ic.lookup(envelopeSenderProperties.getProperty(CONNECTION_FACTORY));
			Queue queue = (Queue) ic.lookup(outboundQueue);

			connection = cf.createConnection();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer produces = session.createProducer(queue);
			//MessageConsumer consumer = session.createConsumer(queue);

			//consumer.setMessageListener(this);
			connection.start();
			
			//List<String> messageList = createMessage();
			
			
			if (listEnvelope == null || listEnvelope.isEmpty()) {
				throw new Exception("NO ENVELOPE FOUND!!!");
			}
			
			logger.info("Sending " + listEnvelope.size() + " messages on " + outboundQueue);

      for (Map.Entry<String, String> entry : listEnvelope.entrySet()) {
 
        TextMessage message = session.createTextMessage(entry.getValue());
        
        //MANUEL: per l'esecuzione del tenant id
       message.setStringProperty("tenantId", "");

        message.setStringProperty("my_jms_header_property", entry.getKey());
        //message.setJMSReplyTo(new DestinationImpl("com/oneleo/platform/outbound"));

        produces.send(message);
        Thread.sleep(50);

      }

		/*	for (int i=0; i<listEnvelope.size();i++) {
				TextMessage message = session.createTextMessage(listEnvelope.get(i));

				message.setStringProperty("my_jms_header_property", "ggg");

				produces.send(message);
				Thread.sleep(50);
				
			}*/
			logger.info("Messages sent: OK");

			//Scanner keyIn = new Scanner(System.in);
			//System.out.print("JMS Server listening. Type a Key + CR to exit\n");
			//keyIn.next();

		} finally {
			if (ic != null) {
				try {
					ic.close();
				} catch (Exception e) {
					throw e;
				}
			}

			// ALWAYS close your connection in a finally block to avoid leaks.
			// Closing connection also takes care of closing its related objects
			// e.g. sessions.
			closeConnection(connection);
		}


	}

	public void receiveMessage() throws Exception {


		//String inboundQueue = envelopeSenderProperties.getProperty(INBOUND_QUEUE);
		String inboundQueue = envelopeSenderProperties.getProperty(envelopeSenderProperties.getProperty(INTERFACE_OPERATION)+"."+INBOUND_QUEUE);

		Context ic = null;
		ConnectionFactory cf = null;
		Connection connection = null;

		try {
			ic = getJMSContext();

			cf = (ConnectionFactory) ic.lookup(envelopeSenderProperties.getProperty(CONNECTION_FACTORY));
			Queue queue = (Queue) ic.lookup(inboundQueue);

			connection = cf.createConnection();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageConsumer consumer = session.createConsumer(queue);

			connection.start();
			TextMessageImpl messageReceived = (TextMessageImpl)consumer.receive(3000);
			//logger.info(""+messageReceived.getText());

			int count=0;
			while (messageReceived != null) {

				count++;
				String xml = messageReceived.getText();
				if (xml != null) {
					Object entity = jaxbMarshaller.unmarshal(xml);
					if (entity instanceof Envelope) {
						Envelope envelope =(Envelope)entity;
						logger.info("Received: "+envelope.getHeader().getStandardHeader().getInterfaceOperation() + "-"+count);
					}
				} else {
					logger.info("NULL -"+count);
				}

				messageReceived = (TextMessageImpl) consumer.receive(3000);
				if (messageReceived == null) {

					Thread.sleep(10000);
					messageReceived = (TextMessageImpl) consumer.receive(3000);
				}
			}
			logger.info("Received message:" + count);




		} finally {
			if (ic != null) {
				try {
					ic.close();
				} catch (Exception e) {
					throw e;
				}
			}
			closeConnection(connection);
		}
	
	}
	
	
	private void clearQueue() throws Exception {

		final String inboundNameQueue = envelopeSenderProperties.getProperty(INBOUND_QUEUE);
		final String outboundNameQueue = envelopeSenderProperties.getProperty(OUTBOUND_QUEUE);

		final Context ic = getJMSContext();
		final ConnectionFactory cf = (ConnectionFactory) ic.lookup(envelopeSenderProperties.getProperty(CONNECTION_FACTORY));

		final Connection connection = cf.createConnection();
		final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		connection.start();

		//CLEAR INBOUND
		Thread threadInbound = new Thread() {
			public void run() {
				try {
					Queue inboundQueue = (Queue) ic.lookup(inboundNameQueue);
					MessageConsumer inboundConsumer = session.createConsumer(inboundQueue);
					TextMessageImpl messageReceived = (TextMessageImpl)inboundConsumer.receive(3000);
					int count=0;
					while (messageReceived != null) {

						count++;
						String xml = messageReceived.getText();
						if (xml != null) {
							Object entity = jaxbMarshaller.unmarshal(xml);
							if (entity instanceof Envelope) {
								Envelope envelope =(Envelope)entity;
								logger.info("Received: "+envelope.getHeader().getStandardHeader().getInterfaceOperation() + "-"+count);
							}
						} else {
							logger.info("NULL -"+count);
						}

						messageReceived = (TextMessageImpl) inboundConsumer.receive(3000);
						if (messageReceived == null) {

							Thread.sleep(10000);
							messageReceived = (TextMessageImpl) inboundConsumer.receive(3000);
						}
					}
					logger.info("Received message:" + count);
				}catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (ic != null) {
						try {
							ic.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					closeConnection(connection);
				}
			}

		};

		//CLEAR OUTBOUND
		Thread threadOutbound = new Thread() {
			public void run() {
				try {
					Queue outboundQueue = (Queue) ic.lookup(outboundNameQueue);
					MessageConsumer outboundConsumer = session.createConsumer(outboundQueue);
					TextMessageImpl messageReceived = (TextMessageImpl)outboundConsumer.receive(3000);
					int count=0;
					while (messageReceived != null) {

						count++;
						String xml = messageReceived.getText();
						if (xml != null) {
							Object entity = jaxbMarshaller.unmarshal(xml);
							if (entity instanceof Envelope) {
								Envelope envelope =(Envelope)entity;
								logger.info("Received: "+envelope.getHeader().getStandardHeader().getInterfaceOperation() + "-"+count);
							}
						} else {
							logger.info("NULL -"+count);
						}

						messageReceived = (TextMessageImpl) outboundConsumer.receive(3000);
						if (messageReceived == null) {

							Thread.sleep(10000);
							messageReceived = (TextMessageImpl) outboundConsumer.receive(3000);
						}
					}
					logger.info("Received message:" + count);
				}catch (Exception e) {
				} finally {
					if (ic != null) {
						try {
							ic.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					closeConnection(connection);
				}
			}

		};
		threadInbound.start();
		threadOutbound.start();

	}



	private void closeConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (JMSException jmse) {
			System.out.println("Could not close connection " + connection + " exception was " + jmse);
		}
	}


	private Map<String, String> getListEnvelope() throws Exception {

		String interfaceType = envelopeSenderProperties.getProperty(INTERFACE_TYPE);
		Map<String, String> listEnvelope = null;

		listEnvelope = getListEnvelopeFromFile();

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
		standardHeader.setInterfaceID(envelopeSenderProperties.getProperty(INTERFACE_ID));
		standardHeader.setCountryID(envelopeSenderProperties.getProperty(COUNTRY_ID));
		standardHeader.setSiteID(envelopeSenderProperties.getProperty(SITE_ID));
		standardHeader.setCompanyID("Alliance");
		standardHeader.setStoreID(envelopeSenderProperties.getProperty(STORE_ID));

		standardHeader.setEntityID(buildBusinessKey(entity));

		header.setStandardHeader(standardHeader);
		envelope.setHeader(header);

		return envelope;

	}

	public Object buildBusinessKey(Object entity) {
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

	private Map<String, String> getListEnvelopeFromFile() throws IOException {

    Map<String, String> listEnvelope = new HashMap<String, String>();

		// System.out.println(envelopeSenderProperties.getProperty(INPUT_FILE_PATH));
		String inputFilePath = envelopeSenderProperties.getProperty(INPUT_FILE_PATH);
		String pathFolder = new File("").getAbsolutePath() + inputFilePath;
		File inputFile = new File(inputFilePath);

		if (inputFile.isDirectory()) {
			for (final File fileEntry : inputFile.listFiles()) {
        final String fileName = fileEntry.getName();
				if (fileName.toUpperCase().endsWith(".XML")) {
					FileReader fReader = new FileReader(fileEntry);

					BufferedReader br = new BufferedReader(fReader);
					StringBuilder xml = new StringBuilder();
					String line = "";
					while ((line = br.readLine()) != null) {
						xml.append(line);
					}
					fReader.close();
					br.close();

          listEnvelope.put(fileName.substring(0, fileName.toUpperCase().indexOf(".XML")), xml.toString());
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
			listEnvelope.put(inputFile.getName(), xml.toString());

		}

		return listEnvelope;
	}

	private Context getJMSContext() throws NamingException {

		envelopeSenderProperties.getProperty(INITIAL_CONTEXT_FACTORY);

		Properties p = new Properties();
		p.put(Context.INITIAL_CONTEXT_FACTORY, envelopeSenderProperties.getProperty(INITIAL_CONTEXT_FACTORY));
		p.put(Context.URL_PKG_PREFIXES,envelopeSenderProperties.getProperty(URL_PKG_PREFIXES));
		p.put(Context.PROVIDER_URL, envelopeSenderProperties.getProperty(PROVIDER_URL));

		InitialContext initialContext = new javax.naming.InitialContext(p);
		return initialContext;
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
				"classpath:config/envelopeSenderNirvana-config.xml" };
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
