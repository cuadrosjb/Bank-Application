package bank.beans;

import java.io.Serializable;
import java.util.logging.Logger;

public class LoanProposalData  implements Serializable {
	private static final Logger Jlog = Logger.getLogger("LoanProposalData");
	String _bankName = null;
	double _loanAmount = 0.0;
	int _paymentScheme = 1;  // 1, 2, 3, 4, 5, 7, 10, 30 year term
	double _interestRate;
	boolean _approved;
	String _reason;
	
	public void LoanProposalData() {
		
	}
	
	public void setBankName(String bankName) {
		_bankName = bankName;
	}
	
	public void setReason(String reason) {
		_reason = reason;
	}
	
	public void setLoanAmount(double loanAmount) {
		_loanAmount = loanAmount;
	}
	
	public void setPaymentScheme(int paymentScheme) {
		_paymentScheme = paymentScheme;
	}
	
	public void setInterestRate(double interestRate) {
		_interestRate = interestRate;
	}
	
	public void setApprovedFlag(boolean bool) {
		_approved = bool;
	}
	
	public String getBankName() {
		return _bankName;
	}
	
	public String getReason() {
		return _reason;
	}
	
	public double getLoanAmount() {
		return _loanAmount;
	}
	
	public int getPaymentScheme() {
		return _paymentScheme;
	}
	
	public double getInterestRate() {
		return _interestRate;
	}
	
	public boolean getApprovedFlag() {
		return _approved;
	}

	@Override
	public String toString() {
		return "Bank Name: " + _bankName + "\rLoan Amount: " + _loanAmount + "$\rPayment Scheme: "
				+ _paymentScheme + "\rInterest Rate: " + _interestRate*100 + "%\r Approved: " + _approved + "\r Reason(s): \n"
				+ _reason;
	}
	
	
	
}