# Role mapping from SAML Attributes

Customizing SAML login has been made easier since some of the plugins APIs have been made
public at maven central:

```xml
<dependency>
    <groupId>com.liferay</groupId>
    <artifactId>com.liferay.saml.api</artifactId>
    <version>5.0.0</version>
	<scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.liferay</groupId>
    <artifactId>com.liferay.saml.persistence.api</artifactId>
    <version>3.0.13</version>
	<scope>provided</scope>
</dependency>
```

> **_NOTE:_**  The above dependencies are the correct one for Liferay SAML Plugin 5.0.0.

Under the hood, the Liferay SAML plugin persists some information relative to the SAML assertion
upon a successful login. This information can be accessed using the SAML persistence API.

Using this line of code, you can access the `SamlSpSession` object, providing an instance of `HttpServletRequest` that includes the cookie used as a key to retrieve that session:

`SamlSpSession samlSpSession = _webSsoProfile.getSamlSpSession(httpServletRequest);`

This call can be made from inside of a successful Post Login action (`LifecycleEvent`):

```java
@Component(
		immediate = true, property = "key=login.events.post",
		service = LifecycleAction.class
	)
public class SamlPostLoginAction implements LifecycleAction {

	@Override
	public void processLifecycleEvent(LifecycleEvent lifecycleEvent) throws ActionException {

		SamlSpSession samlSpSession = _webSsoProfile.getSamlSpSession(lifecycleEvent.getRequest());
    
	}
    
	@Reference
	private WebSsoProfile _webSsoProfile;
}
```

One interesting piece of information held by the `SamlSPSession` is the decrypted
SAML Assertion. The SAML Liferay plugin does a great job identifying the IDP who delivered
the SAML Assertion, validating it against the SP configuration (including signature checks),
checking whether the Assertion is encrypted and decrypt it if required...

Well, it's a great thing being able to access the SAML Assertion through this API because
you know it has been properly processed if it has reached that point!

In the past, developers have also tried implementing a Servlet Filter on `/c/portal/saml/acs` where
it is possible to intercept the *SAMLResponse* from the HTTP POST Form data.
That *SAMLResponse* is Base64 encoded.
However, this approach has some limitations and issues:
* The *SAMLResponse* might be encrypted by the IDP (developers would probably have given up if facing this issue or asked the IDP not to encrypt the assertion)
* There might be several IDPs delivering SAML Assertions (developers would have had to add some additional IDP mapping logic to manage that)
* The SAMLResponse might have been forged: better postpone any operation you perform on the user
as long as the login has not been successful (store the information gathered from the SAMLResponse in some buffer before using it in the Post Login action)

Good thing you do not have to manage this: this is the added value of the SAML plugin!

Once you've reached that point, there are many things you can do with the information contained
in the SAML Assertion. For example, you could retrieve a list of **Roles** and add them to the
**User** who logged in. You could also populate some additional **User** fields that cannot
be mapped through the SAML IDP Connection configuration (where you can only map first name, last
name, email and screen name).

This example shows how you can retrieve information from the SAML assertions using some XPath.
It is then up to you to choose which Liferay APIs you want to call depending on what you'd have
to do.

```java
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
```

The following XPath expression means "Please fetch all `Attribute` tags which have an attribute called `Name` whose value equals `'Role'`, and provide their `AttributeValue` child node".

```java
String expression = "//Attribute[@Name='Role']/AttributeValue";
```

XPath is a great way to avoid writing complex tree navigation algorithms in java code (which is neither fun to write nor to read).
