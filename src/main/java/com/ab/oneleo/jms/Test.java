package com.ab.oneleo.jms;

import java.net.URI;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.interceptor.Fault;

public class Test implements SoapInterceptor{

	public static void main(String[] args) {

		
		System.out.println(QName.class.getName());
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleFault(SoapMessage message) {
		// TODO Auto-generated method stub
		//message.GET
		
	}

	@Override
	public Set<URI> getRoles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<QName> getUnderstoodHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

}
