/**
 *
 * mmlRppFacility.java
 * Created on 2002/7/30 10:0:36
 */
package jp.ac.kumamoto_u.kuh.fc.jsato.math_mml;

import java.awt.*;
import java.util.*;
import org.xml.sax.*;

import java.io.*;
/**
 *
 * @author	Junzo SATO
 * @version
 */
public class mmlRppFacility extends MMLObject {
	
	/* fields */
	private String __mmlRpfacilityCode = null;
	private String __mmlRpfacilityCodeId = null;

	private String text = null;
	
	public mmlRppFacility() {
	}
	
	
	/* print */
	public void printObject(PrintWriter pw, MMLVisitor visitor) throws IOException {
		if ( this.getQName() != null ) {
			visitor.goDown();// adjust tab
			pw.print( visitor.getTabPadding() + "<" + this.getQName() );
			/* print atts */
			if ( __mmlRpfacilityCode != null ) pw.print(" " + "mmlRp:facilityCode" +  "=" + "'" + __mmlRpfacilityCode + "'");
			if ( __mmlRpfacilityCodeId != null ) pw.print(" " + "mmlRp:facilityCodeId" +  "=" + "'" + __mmlRpfacilityCodeId + "'");

			if ( this.getLocalName().equals("Mml") ) {
				visitor.printNamespaces(pw);
			}
			pw.print( ">" );
			/* print content */
			// this element need not to print tab padding before the closing tag.
			visitor.setIgnoreTab( true );
			if (text != null) {
				if ( this.getText().equals("") == false ) pw.print( this.getText() );
			}

			// only compound element requires to add tab padding before closing tag
			if ( visitor.getIgnoreTab() == false ) {
				pw.print( visitor.getTabPadding() );
			}
			pw.print( "</" + this.getQName() + ">\n" );
			pw.flush();
			visitor.setIgnoreTab( false );
			visitor.goUp();// adjust tab
		}
	}
	
	public boolean buildStart(String namespaceURI, String localName, String qName, Attributes atts, MMLBuilder builder) {
		if (qName.equals("mmlRp:pFacility") == true) {
			super.buildStart(namespaceURI,localName,qName,atts,builder);
			printlnStatus(parentElement.getQName() + " " + qName);
			
			/* create tree node */
			mmlRppFacility obj = new mmlRppFacility();
			builder.getMmlTree().addElement( obj );
			obj.setParentIndex( builder.mmlTreeIndex );
			builder.adjustIndex();
			((mmlRppFacility)builder.getElement()).setNamespace( getNamespace() );
			((mmlRppFacility)builder.getElement()).setLocalName( getLocalName() );
			((mmlRppFacility)builder.getElement()).setQName( getQName() );
			((mmlRppFacility)builder.getElement()).setAtts( getAtts() );/* :-) */
			/* atts */
			if (atts != null) {
				setMmlRpfacilityCode( atts.getValue(namespaceURI, "facilityCode") );
				((mmlRppFacility)builder.getElement()).setMmlRpfacilityCode( atts.getValue(namespaceURI, "facilityCode") );
				setMmlRpfacilityCodeId( atts.getValue(namespaceURI, "facilityCodeId") );
				((mmlRppFacility)builder.getElement()).setMmlRpfacilityCodeId( atts.getValue(namespaceURI, "facilityCodeId") );
			}

			
			return true;
		}
		return false;
	}
	
	public boolean buildEnd(String namespaceURI, String localName, String qName, MMLBuilder builder) {
		if (qName.equals("mmlRp:pFacility") == true) {
			
			/* connection */
			if (parentElement.getQName().equals("mmlRp:perform")) {
				((mmlRpperform)builder.getParent()).setPFacility((mmlRppFacility)builder.getElement());
			}

			
			printlnStatus(parentElement.getQName()+" /"+qName);


			builder.restoreIndex();
			super.buildEnd(namespaceURI,localName,qName,builder);
			return true;
		}
		return false;
	}
	
	/* characters */
	public boolean characters(char[] ch, int start, int length, MMLBuilder builder) {
		if (builder.getCurrentElement().getQName().equals("mmlRp:pFacility")) {
			StringBuffer buffer=new StringBuffer(length);
			buffer.append(ch, start, length);
			setText(buffer.toString());
			((mmlRppFacility)builder.getElement()).setText( getText() );
			printlnStatus(parentElement.getQName()+" "+this.getQName()+":"+this.getText());
			return true;
		}
		return false;
	}
	
	
	/* setters and getters */
	public void setMmlRpfacilityCode(String __mmlRpfacilityCode) {
		this.__mmlRpfacilityCode = __mmlRpfacilityCode;
	}
	public String getMmlRpfacilityCode() {
		return __mmlRpfacilityCode;
	}
	public void setMmlRpfacilityCodeId(String __mmlRpfacilityCodeId) {
		this.__mmlRpfacilityCodeId = __mmlRpfacilityCodeId;
	}
	public String getMmlRpfacilityCodeId() {
		return __mmlRpfacilityCodeId;
	}

	public void setText(String text) {
		this.text = text;
	}
	public String getText() {
		return text;
	}
	
}