package com.sample.adaptivepayments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.paypal.exception.ClientActionRequiredException;
import com.paypal.exception.HttpErrorException;
import com.paypal.exception.InvalidCredentialException;
import com.paypal.exception.InvalidResponseDataException;
import com.paypal.exception.MissingCredentialException;
import com.paypal.exception.SSLConfigurationException;
import com.paypal.sdk.exceptions.OAuthException;
import com.paypal.svcs.services.AdaptivePaymentsService;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;
import com.paypal.svcs.types.ap.RefundInfo;
import com.paypal.svcs.types.ap.RefundRequest;
import com.paypal.svcs.types.ap.RefundResponse;
import com.paypal.svcs.types.common.PhoneNumberType;
import com.paypal.svcs.types.common.RequestEnvelope;

/**
 * Servlet implementation class RefundServlet
 */
public class RefundServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RefundServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		getServletConfig().getServletContext()
				.getRequestDispatcher("/Refund.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		HttpSession session = request.getSession();
		session.setAttribute("url", request.getRequestURI());
		session.setAttribute(
				"relatedUrl",
				"<ul><li><a href='Pay'>Pay</a></li><li><a href='PaymentDetails'>PaymentDetails</a></li><li><a href='GetPaymentOptions'>GetPaymentOptions</a></li><li><a href='ExecutePayment'>ExecutePayment</a></li><li><a href='SetPaymentOptions'>SetPaymentOptions</a></li></ul>");
		RequestEnvelope requestEnvelope = new RequestEnvelope("en_US");
		RefundRequest req = new RefundRequest(requestEnvelope);
		//The key used to create the payment that you want to refund
		if (request.getParameter("payKey") != "")
			req.setPayKey(request.getParameter("payKey"));
		/*
		 * The currency code. You must specify the currency code that matches
		 * the currency code of the original payment unless you also specify the payment key
		 */
		req.setCurrencyCode(request.getParameter("currencyCode"));
		/*
		 * A PayPal transaction ID associated with the receiver whose payment 
		 * you want to refund to the sender. Use field name characters exactly as shown.
		 */
		if (request.getParameter("transactionID") != "")
			req.setTransactionId(request.getParameter("transactionID"));
		//(Optional) The tracking ID associated with the payment that you want to refund. 
		if (request.getParameter("trackingID") != "")
			req.setTrackingId(request.getParameter("trackingID"));
		
		List<Receiver> receiver = new ArrayList<Receiver>();

		Receiver rec = new Receiver();
		//(Required) Amount to be credited to the receiver's account.
		if (request.getParameter("amount") != "")
			rec.setAmount(Double.parseDouble(request.getParameter("amount")));
		//(Required) Receiver's email address.
		if (request.getParameter("mail") != "")
			rec.setEmail(request.getParameter("mail"));
		//Optional) This fields is not used.
		if (request.getParameter("invoiceID") != "")
			rec.setInvoiceId(request.getParameter("invoiceID"));
		/*
		 * (Optional) A type to specify the receiver's phone number. 
		 * The PayRequest must pass either an email address or a phone number as the payment receiver.
		 */
		if (request.getParameter("phoneNumber") != "") {
			PhoneNumberType phone = new PhoneNumberType(
					request.getParameter("countryCode"),
					request.getParameter("phoneNumber"));
			phone.setExtension(request.getParameter("extension"));
			rec.setPhone(phone);
		}
		/*
		 *  (Optional) Whether this receiver is the primary receiver, 
		 *  which makes this a refund for a chained payment. 
		 *  You can specify at most one primary receiver. 
		 *  Omit this field for refunds for simple and parallel payments.
			Allowable values are:
			    true � Primary receiver
			    false � Secondary receiver (default)
		 */
		if (request.getParameter("setPrimary") != "")
			rec.setPrimary(Boolean.parseBoolean(request
					.getParameter("setPrimary")));
		receiver.add(rec);
		ReceiverList receiverlst = new ReceiverList(receiver);
		req.setReceiverList(receiverlst);
		
		AdaptivePaymentsService service = new AdaptivePaymentsService(this
				.getClass().getResourceAsStream("/sdk_config.properties"));
		try {
			RefundResponse resp = service.refund(req);
			response.setContentType("text/html");
			if (resp != null) {
				session.setAttribute("RESPONSE_OBJECT", resp);
				session.setAttribute("lastReq", service.getLastRequest());
				session.setAttribute("lastResp", service.getLastResponse());
				if (resp.getResponseEnvelope().getAck().toString()
						.equalsIgnoreCase("SUCCESS")) {
					Map<Object, Object> map = new LinkedHashMap<Object, Object>();
					map.put("Ack", resp.getResponseEnvelope().getAck());
					map.put("Correlation ID", resp.getResponseEnvelope()
							.getCorrelationId());
					map.put("Time Stamp", resp.getResponseEnvelope()
							.getTimestamp());
					Iterator<RefundInfo> iterator = resp.getRefundInfoList()
							.getRefundInfo().iterator();
					int index = 1;
					while (iterator.hasNext()) {
						RefundInfo refundInfo = iterator.next();
						map.put("Refund Transaction ID" + index,
								refundInfo.getEncryptedRefundTransactionId());
						map.put("Net Amount" + index,
								refundInfo.getRefundNetAmount());
						map.put("Refund Status" + index,
								refundInfo.getRefundStatus());
						index++;
					}
					session.setAttribute("map", map);
					response.sendRedirect("Response.jsp");
				} else {
					session.setAttribute("Error", resp.getError());
					response.sendRedirect("Error.jsp");
				}
			}
		} catch (SSLConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidCredentialException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HttpErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidResponseDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientActionRequiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MissingCredentialException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
