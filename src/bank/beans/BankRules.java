package bank.beans;


public class BankRules {
	
	private static final String BANK_LAST_NAME = "CUADROS";
	private static final double MAX_INTEREST_RATE = .10;
	private static final int MAX_AMOUNT_LIMIT = 500000;
	private static final double MAX_SALARY_RATIO = 3.0;
	
	private String borrower;
	private String reason;
	private double ratio;
	//private double salary;
	private int paymentScheme;
	private double fedInterest;
	private double loanAmount;
	
	
	
	private LoanProposalData lpd;
	
	public BankRules(String borrower,double salary, double loanAmount, int paymentScheme){
		lpd = new LoanProposalData();
		this.borrower = borrower;
		this.loanAmount = loanAmount;
		//this.salary = salary;
		this.ratio = (salary/loanAmount);
		this.paymentScheme = paymentScheme;
		this.fedInterest = getFedInterest();
	}

	public LoanProposalData canWeMakeItWork(){
		lpd.setBankName(BANK_LAST_NAME);
		lpd.setLoanAmount(loanAmount);
				
		if(loanAmount > MAX_AMOUNT_LIMIT){ 
			lpd.setApprovedFlag(false);
			reason = "Amount is too high.\n";
		}
		if(borrower.toUpperCase().contains(BANK_LAST_NAME)){
			lpd.setApprovedFlag(false);
			reason += "Bank regulations prohibits loans to family members.\n";
		}
		if(ratio > MAX_SALARY_RATIO){
			lpd.setApprovedFlag(false);
			reason += "The loan amount to salary ratio exceeds 3.0.\n";
		}
		lpd.setInterestRate(proposedInterestRate());
		lpd.setPaymentScheme(approvePaymentScheme());
		lpd.setReason(reason);
		
			
		return lpd;
	}
	
	public double proposedInterestRate(){
		boolean notSatisfied = true;
		double newInterestRate = MAX_INTEREST_RATE;
		
		while(notSatisfied){
			if(getInterestRateAccordingtoTerm() > MAX_INTEREST_RATE){
				newInterestRate -= .01;  
			}else  if(getInterestRateAccordingtoTerm() < fedInterest){
				newInterestRate += .01;  
			}else{
				notSatisfied = false;
			}
		}
		return newInterestRate;
	}
	
	public double getFedInterest(){
		
		return new GetFedInterest().getFedInterest();
	}
	
	public double getInterestRateAccordingtoTerm(){
		
		switch(paymentScheme){
		case 1:
			return 0.015;
		case 2:
			return 0.025;
		case 3:
			return 0.035;
		case 4:
			return 0.045;
		case 5:
			return 0.055;
		case 7:
			return 0.078;
		case 10:
			return 0.078;
		case 30:
			return 0.088;
		default:
			return 0.01;
		}
	}
	
	public int approvePaymentScheme(){
		if(1%paymentScheme == 0 ||
				2%paymentScheme == 0 ||
				3%paymentScheme == 0 ||
				4%paymentScheme == 0 ||
				5%paymentScheme == 0 ||
				7%paymentScheme == 0 ||
				10%paymentScheme== 0 ||
				30%paymentScheme== 0){
			return paymentScheme;
			
		}else if(8%paymentScheme == 0 || 9%paymentScheme == 0){
			if(ratio < .15) 
				return 7;
			else 
				return 10;
			
		}else if(10<paymentScheme && paymentScheme < 20){
			if(ratio < .20) 
				return 10;
			else 
				return 30;
		}else {
			return 30;
		}
	}

}
