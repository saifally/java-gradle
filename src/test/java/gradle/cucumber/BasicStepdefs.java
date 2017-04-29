package gradle.cucumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.MQQueue;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueueSender;
import com.ibm.msg.client.wmq.common.CommonConstants;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import gradle.MessageData;

public class BasicStepdefs {

	private static String conname = "localhost";
	private static String port = "1411";
	private static String channel = "SYSTEM.APP.SVRCONN";
	private static String qmgr = "QM2";
	private MQQueueConnectionFactory qcf;
	private QueueConnection queueCon;
	private QueueSession queueSession;

	public BasicStepdefs() throws NumberFormatException, JMSException {
		qcf = new MQQueueConnectionFactory();
		qcf.setHostName(conname);
		qcf.setPort(Integer.parseInt(port));
		qcf.setQueueManager(qmgr);
		qcf.setChannel(channel);
		qcf.setTransportType(CommonConstants.WMQ_CM_CLIENT);
	}

	@Given("^I send a message on input queue$")
	public void i_send_a_message_on_input_queue(List<MessageData> data) throws Throwable {

		String messageText = data.get(0).message;
		System.out.println(data.get(0).message);

		queueCon = qcf.createQueueConnection();
		queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		MQQueue queue = (MQQueue) queueSession.createQueue("queue:///SAIF.QUEUE.1");
		MQQueueSender sender = (MQQueueSender) queueSession.createSender(queue);

		JMSTextMessage message = (JMSTextMessage) queueSession.createTextMessage(messageText);

		queueCon.start();

		System.out.println("before Sent message:\\n" + message);

		sender.send(message);
		System.out.println("Sent message:\\n" + message);

		sender.close();

		queueSession.close();
		queueCon.close();

	}

	@Then("^I receive a message on output queue$")
	public void i_receive_a_message_on_output_queue(DataTable expectedData) throws Throwable {

		List<MessageData> data = new ArrayList<>();

		try {

			queueCon = qcf.createQueueConnection();
			queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			MQQueue queue = (MQQueue) queueSession.createQueue("queue:///SAIF.QUEUE.1");
			MessageConsumer consumer = queueSession.createConsumer(queue);

			boolean readMessage = true;
			queueCon.start();
			while (readMessage) { // run forever
				javax.jms.Message msg = consumer.receive(); // blocking! (more)
				if (!(msg instanceof TextMessage))
					throw new RuntimeException("Expected a TextMessage");
				TextMessage tm = (TextMessage) msg;
				System.out.println(tm.getText());

				if (tm.getText() != "") {
					data.add(new MessageData(tm.getText()));
					// print message content
					readMessage = false;
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		} finally {
			try {
				queueCon.close();
			} catch (JMSException e) {
				e.printStackTrace();
			} // free all resources (more)
		}
		
		System.out.println(data);
		System.out.println(expectedData.getGherkinRows());
		
//		expectedData.diff(data);

	}
}
