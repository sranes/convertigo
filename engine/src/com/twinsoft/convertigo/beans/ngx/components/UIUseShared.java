/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.ngx.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class UIUseShared extends UIElement {

	private static final long serialVersionUID = -6355983207888274045L;

	public UIUseShared() {
		super();
	}

	@Override
	public UIUseShared clone() throws CloneNotSupportedException {
		UIUseShared cloned = (UIUseShared) super.clone();
		cloned.target = null;
		return cloned;
	}
	
	private String sharedcomponent = "";
	
	public String getSharedComponentQName() {
		return sharedcomponent;
	}

	public void setSharedComponentQName(String sharedComponent) {
		this.sharedcomponent = sharedComponent;
	}

	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		if (!(uiComponent instanceof UIUseVariable) && !(uiComponent instanceof UIControlEvent) && !(uiComponent instanceof UIAttribute)) {
			throw new EngineException("You can not add this component to a UIUseShared component!");
		}
		
		super.addUIComponent(uiComponent, after);
	}
	
	@Override
	public List<UIComponent> getUIComponentList() {
		return super.getUIComponentList();
	}
	
	public UIUseVariable getVariable(String variableName) {
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIUseVariable) {
				UIUseVariable variable = (UIUseVariable)component;
				if (variable.getName().equals(variableName)) {
					return variable;
				}
			}
		}
		return null;
	}
	
	public String getEventAttr(String eventName) {
		if (!eventName.isBlank()) {
			if (!eventName.startsWith("(") && !eventName.endsWith(")")) {
				return "("+ eventName +")";
			}
		}
		return eventName;
	}
	
	public UIControlEvent getEvent(String eventName) {
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIControlEvent) {
				UIControlEvent event = (UIControlEvent)component;
				if (event.getEventName().equals(eventName)) {
					return event;
				}
			}
		}
		return null;
	}
	
	public List<String> getEventNames() {
		List<String> list = new ArrayList<String>();
		UISharedComponent targetSharedComp = this.getTargetSharedComponent();
		if (targetSharedComp != null) {
			for (UICompEvent uice: targetSharedComp.getUICompEventList()) {
				String eventName = uice.getAttrName();
				if (!eventName.isBlank()) {
					if (!list.contains(eventName)) {
						list.add(eventName);
					}
				}
			}
		}
		return list;
	}
	
	public boolean isRecursive() {
		UISharedComponent parentSharedComp = ((UIUseShared)this.getOriginal()).getSharedComponent();
		// if UIUseShared is in a UISharedComponent
		if (parentSharedComp != null) {
			UISharedComponent targetSharedComp = this.getTargetSharedComponent();
			// if UIUseShared has a target UISharedComponent
			if (targetSharedComp != null) {
				// if they are the same
				if (parentSharedComp.priority == targetSharedComp.priority) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public String computeTemplate() {
		String computed = "";
		if (isEnabled()) {
			UISharedComponent uisc = getTargetSharedComponent();
			if (uisc != null && uisc.isEnabled()) {
				if (uisc.isRegular()) {
					StringBuilder eventBindings = new StringBuilder();
					StringBuilder attrclasses = new StringBuilder();
					StringBuilder params = new StringBuilder();
					for (UIComponent uic: getUIComponentList()) {
						// Overridden component variables
						if (uic instanceof UIUseVariable) {
							UIUseVariable uiuv = (UIUseVariable)uic;
							if (uiuv.isEnabled()) {
								params.append(uiuv.computeTemplate());
							}
						}
						// Overridden event bindings
						else if (uic instanceof UIControlEvent) {
							UIControlEvent uice = (UIControlEvent)uic;
							if (uice.isEnabled()) {
								eventBindings.append(uice.computeTemplate());
							}
						}
						// Add attributes (class,..)
						else if (uic instanceof UIAttribute) {
							UIAttribute uiAttribute = (UIAttribute)uic;
							if (uiAttribute.isEnabled()) {
								if (uiAttribute.getAttrName().equals("class")) {
									attrclasses.append(attrclasses.length()>0 ? " ":"").append(uiAttribute.getAttrValue());
								} else {
									params.append(uiAttribute.computeTemplate());
								}
							}
						}
					}
					
					//String scope = getScope();
					
					String compSelector = uisc.getSelector();
					String compIdentifier = "#"+ uisc.getIdentifier();
					String useIdentifier = this.getIdentifier().isBlank() ? "":"#"+ this.getIdentifier();
					String identifiers = compIdentifier + " " + useIdentifier;
					String classes = attrclasses.length() > 0 ? "class=\""+attrclasses+"\"": "";
					computed += "<"+compSelector+" "+ identifiers +" [owner]=\"this\" "+params+" "+classes+" "+eventBindings +"></"+compSelector+">" + System.lineSeparator();
				}
			}
		}
		return computed;
	}
	
	protected String getScope() {
		UIUseShared original = (UIUseShared) getOriginal();
		UISharedComponent sharedComponent = original.getSharedComponent();
		boolean isInSharedComponent = sharedComponent  != null;

		String scope = "";
		
		DatabaseObject parent = getParent();
		while (parent != null && !(parent instanceof UIAppEvent) && !(parent instanceof UIPageEvent) && !(parent instanceof UISharedComponentEvent) && !(parent instanceof UIEventSubscriber)) {
			if (parent instanceof UIUseShared) {
				UISharedComponent uisc = ((UIUseShared) parent).getTargetSharedComponent();
				if (uisc != null) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += "comp"+uisc.priority + ": "+ "comp"+uisc.priority;
				}
				if (isInSharedComponent) {
					break;
				}
			}
			if (parent instanceof UIControlDirective) {
				UIControlDirective uicd = (UIControlDirective)parent;
				if (AttrDirective.ForEach.equals(AttrDirective.getDirective(uicd.getDirectiveName()))) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += "item"+uicd.priority + ": "+ "item"+uicd.priority;
					
					String item = uicd.getDirectiveItemName();
					if (!item.isEmpty()) {
						scope += !scope.isEmpty() ? ", ":"";
						scope += item + ": "+ item;
					}
					String index = uicd.getDirectiveIndexName();
					if (!index.isEmpty()) {
						scope += !scope.isEmpty() ? ", ":"";
						scope += index + ":" + index;
					}
				}
			}
			if (parent instanceof UIElement) {
				String identifier = ((UIElement)parent).getIdentifier();
				if (!identifier.isEmpty()) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += identifier+ ": "+ identifier;
				}			
			}
			
			parent = parent.getParent();
		}
		
		if (!scope.isEmpty()) {
			if (isInSharedComponent) {
				scope = "merge({}, {"+ scope +"})";
			} else {
				scope = "merge({}, {"+ scope +"})";
			}
		} else {
			scope = "{}";
		}
		
		return scope;
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			if (!isRecursive()) {
				IScriptComponent main = getMainScriptComponent();
				if (main != null && uisc.isRegular()) {
					try {
						String imports = jsonScripts.getString("imports");
						if (main.addImport("ViewChild", "@angular/core")) {
							imports += "import { ViewChild } from '@angular/core';" + System.lineSeparator();
						}
						if (main.addImport("ViewChildren", "@angular/core")) {
							imports += "import { ViewChildren } from '@angular/core';" + System.lineSeparator();
						}
						if (main.addImport("QueryList", "@angular/core")) {
							imports += "import { QueryList } from '@angular/core';" + System.lineSeparator();
						}
						jsonScripts.put("imports", imports);
						
						String declarations = jsonScripts.getString("declarations");
						String dname = uisc.getIdentifier();
						String dcode = "@ViewChild(\""+ dname +"\", { static: false }) public "+ dname+";";
						if (main.addDeclaration(dname, dcode)) {
							declarations += System.lineSeparator() + "\t" + dcode;
						}
						String all_dname = "all_" + dname;
						String all_dcode = "@ViewChildren(\""+ dname +"\") public "+ all_dname+" : QueryList<any>;";
						if (main.addDeclaration(all_dname, all_dcode)) {
							declarations += System.lineSeparator() + "\t" + all_dcode;
						}
						if (!this.getIdentifier().isBlank()) {
							dname = this.getIdentifier();
							dcode = "@ViewChild(\""+ dname +"\", { static: false }) public "+ dname+";";
							if (main.addDeclaration(dname, dcode)) {
								declarations += System.lineSeparator() + "\t" + dcode;
							}
							all_dname = "all_" + dname;
							all_dcode = "@ViewChildren(\""+ dname +"\") public "+ all_dname+" : QueryList<any>;";
							if (main.addDeclaration(all_dname, all_dcode)) {
								declarations += System.lineSeparator() + "\t" + all_dcode;
							}
						}
						jsonScripts.put("declarations", declarations);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					for (UIComponent uic: getUIComponentList()) {
						uic.computeScripts(jsonScripts);
					}
				}
			}
		}
	}

	@Override
	public String computeStyle() {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			if (!isRecursive()) {
				return uisc.computeStyle(this);
			}
		}
		return "";
	}
	
	
	@Override
	public void addPageEvent(Set<UIComponent> done, List<UIPageEvent> eventList) {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			if (!done.add(this)) {
				return;
			}
			if (!isRecursive()) {
				uisc.addPageEvent(this, done, eventList);
			}
		}
	}

	
	@Override
	public void addEventSubscriber(Set<UIComponent> done, List<UIEventSubscriber> eventList) {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			if (!done.add(this)) {
				return;
			}
			if (!isRecursive()) {
				uisc.addEventSubscriber(this, done, eventList);
			}
		}
	}

	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		if(!done.add(this)) {
			return;
		}
		for (UIComponent uic : getUIComponentList()) {
			uic.addContributors(done, contributors);
		}
		
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			if (!isRecursive()) {
				uisc.addContributors(this, done, contributors);
			}
		}
	}
	
	@Override
	protected void addInfos(Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			if (!done.add(this)) {
				return;
			}
			if (!isRecursive()) {
				uisc.addInfos(this, done, infoMap);
			}
		}
	}
	
	transient private UISharedComponent target = null;
	
	public UISharedComponent getTargetSharedComponent() {
		String qname =  getSharedComponentQName();
		if (target == null || !target.getQName().equals(qname)) {
			target = null;
			if (qname.indexOf('.') != -1) {
				String p_name = qname.substring(0, qname.indexOf('.'));
				Project project = this.getProject();
				if (project != null) {
					Project p = null;
					try {
						p = Engine.theApp.referencedProjectManager.importProjectFrom(project, p_name);
						if (p == null) {
							throw new Exception();
						}
					} catch (Exception e) {
						Engine.logBeans.warn("(UIUseShared) For \""+  this.toString() +"\", targeted project \""+ p_name +"\" is missing !");
					}
					if (p != null) {
						if (p.getMobileApplication() != null) {
							try {
								ApplicationComponent app = (ApplicationComponent) p.getMobileApplication().getApplicationComponent();
								if (app != null) {
									for (UISharedComponent uisc: app.getSharedComponentList()) {
										if (uisc.getQName().equals(qname)) {
											target = uisc;
											break;
										}
									}
								}
							} catch (ClassCastException e) {
								Engine.logBeans.warn("(UIUseShared) For \""+  this.toString() +"\", targeted component \""+ qname +"\" is not compatible !");
							}
						} else {
							Engine.logBeans.warn("(UIUseShared) For \""+  this.toString() +"\", targeted project \""+ p_name +"\" does not contain any mobile application !");
						}
						
						if (target == null) {
							Engine.logBeans.warn("(UIUseShared) For \""+  this.toString() +"\", targeted component \""+ qname +"\" is missing !");
						}
					}
				}
			} else {
				Engine.logBeans.warn("(UIUseShared) Component \""+ this.toString() +"\" has no target shared component defined !");
			}
		}
		return target;
	}
	
	@Override
	public String toString() {
		String compName = this.sharedcomponent.isEmpty() ? "?" : this.sharedcomponent.substring(this.sharedcomponent.lastIndexOf('.') + 1);
		return "use " + compName + (identifier.isBlank() ? "":" #"+identifier);
	}

}
