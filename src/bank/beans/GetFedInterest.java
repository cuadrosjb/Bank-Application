package bank.beans;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class GetFedInterest implements MessageListener {

	
	private TopicSession pubSession;
	private TopicSubscriber subscriber;
	private TopicConnection tConnection;
	private String fedTopic = "topic.fed.interest";
	
	private double fedInterest = 0.0;
	private boolean loaded = false;
	private int paymentScheme;
	
	public double getFedInterest(){
		while(!loaded){	}
		System.out.println("Returning the federal interest");
		return fedInterest;
	}
	
	public GetFedInterest(int paymentScheme){
		Context ctx;
		this.paymentScheme = paymentScheme;
		try {
			ctx = new InitialContext();
			setupTopicConnection(ctx);
		} catch (NamingException | JMSException e) {
		
			e.printStackTrace();
		}
		
	}
	public void onMessage(Message message) {

		try {
   
            	long millis = ((StreamMessage)message).readLong();                     // Time in millisecond   

                double one_year_rate= ((StreamMessage)message).readDouble();   // 1-year 

                double two_year_rate= ((StreamMessage)message).readDouble();   // 2-year 

                double three_year_rate= ((StreamMessage)message).readDouble(); // 3-year

                double four_year_rate= ((StreamMessage)message).readDouble();  // 4-year

                double five_year_rate= ((StreamMessage)message).readDouble();  // 5-year

                double seven_year_rate= ((StreamMessage)message).readDouble(); // 7-year

                double ten_year_rate= ((StreamMessage)message).readDouble();   // 10-year

                double thirty_year_rate= ((StreamMessage)message).readDouble(); // 30-year
                
                if(paymentScheme == 1){
                	fedInterest = one_year_rate;
                }else if(paymentScheme == 2){
                	fedInterest = two_year_rate;
                }else if(paymentScheme == 3){
                	fedInterest = three_year_rate;
                }else if(paymentScheme == 4){
                	fedInterest = four_year_rate;
                }else if(paymentScheme == 5){
                	fedInterest = five_year_rate;
                }else if(paymentScheme == 7){
                	fedInterest = seven_year_rate;
                }else if(paymentScheme == 10){
                	fedInterest = ten_year_rate;
                }else{
                	fedInterest = thirty_year_rate;
                }
           
                System.out.println("StreamMessage loaded " + millis);
                loaded = true;
			
			
            	
            
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void setupTopicConnection(Context ctx) throws NamingException, JMSException{
		
		TopicConnectionFactory tFactory = (TopicConnectionFactory) ctx.lookup("ECSU.CSC360.TCF");
		
		tConnection = tFactory.createTopicConnection();

		pubSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		Topic borrowTopic = (Topic)ctx.lookup(fedTopic);
		
		subscriber = pubSession.createSubscriber(borrowTopic);
		
		subscriber.setMessageListener(this);
		
		tConnection.start();
		
	}
}
