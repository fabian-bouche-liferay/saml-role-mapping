package com.imolczek.liferay.lab.role.mapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.events.LifecycleEvent;
import com.liferay.saml.persistence.model.SamlSpSession;
import com.liferay.saml.persistence.service.SamlSpSessionLocalService;
import com.liferay.saml.runtime.servlet.profile.WebSsoProfile;

@Component(
		immediate = true, property = "key=login.events.post",
		service = LifecycleAction.class
	)
public class SamlRoleMappingPostLoginAction implements LifecycleAction {

	private static final Logger LOG = LoggerFactory.getLogger(
			SamlRoleMappingPostLoginAction.class);
	
	@Override
	public void processLifecycleEvent(LifecycleEvent lifecycleEvent) throws ActionException {

		SamlSpSession samlSpSession = _webSsoProfile.getSamlSpSession(lifecycleEvent.getRequest());
		
		try {
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			ByteArrayInputStream input = new ByteArrayInputStream(
					samlSpSession.getAssertionXml().getBytes("UTF-8"));
			
			Document assertionsDOM = builder.parse(input);
			
			input.close();
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			String expression = "//Attribute[@Name='Role']/AttributeValue";
			NodeList roles = (NodeList) xPath.compile(expression).evaluate(assertionsDOM, XPathConstants.NODESET);

			LOG.info("Number role attributes found: " + roles.getLength());

			for(int i = 0; i < roles.getLength(); i++) {
				
				Node curRole = roles.item(i);

				LOG.info("Role: " + curRole.getTextContent());
				
			}
			
		} catch (ParserConfigurationException e) {
			LOG.error("Parser configuration exception", e);
		} catch (UnsupportedEncodingException e) {
			LOG.error("Unable to support UTF-8", e);
		} catch (SAXException e) {
			LOG.error("Failed to parse XML", e);
		} catch (IOException e) {
			LOG.error("Failed to read XML", e);
		} catch (XPathExpressionException e) {
			LOG.error("Malformed XPath request", e);
		}
		
	}
	
	@Reference
	private WebSsoProfile _webSsoProfile;
	
	@Reference
	private SamlSpSessionLocalService _samlSpSessionLocalService;

}
