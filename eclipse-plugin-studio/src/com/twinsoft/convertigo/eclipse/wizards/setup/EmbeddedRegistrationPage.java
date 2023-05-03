/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.swt.RegistrationBrowser;

class EmbeddedRegistrationPage extends WizardPage {
	
	private RegistrationBrowser browser;
	
	public EmbeddedRegistrationPage () {
		super("EmbeddedRegistrationPage");
		setTitle("Register or Login now");
		setDescription("Register or login...");
	}

	@Override
	public IWizard getWizard() {
		setErrorMessage(null);
		setMessage(getDescription());
		
		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());

		return super.getWizard();
	}
	
	public void createControl(final Composite parent) {
		GridData gd;
		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(2, false));
		
		browser = new RegistrationBrowser(root, SWT.NONE);
		browser.setLayoutData(gd = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		gd.heightHint = 370;
		gd.widthHint = 700;
		browser.onPSC(psc -> {
			try {
				ConvertigoPlugin.decodePsc(psc);
				SetupWizard wizard = (SetupWizard) getWizard();
				wizard.psc = psc;
				browser.getDisplay().asyncExec(() -> {
					wizard.performFinish();
				});
			} catch (PscException exception) {
				setErrorMessage(exception.getMessage());
				setPageComplete(false);
			}
		}).goRegister();
		
		Button havePSC = new Button(root, SWT.CHECK);
		havePSC.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(havePSC.getSelection());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		Label label = new Label(root, SWT.NONE);
		label.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseDown(MouseEvent e) {
				havePSC.setSelection(!havePSC.getSelection());
				havePSC.notifyListeners(SWT.Selection, null);
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		label.setText("I want to paste my PSC");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		root.pack();
		setControl(root);
		setPageComplete(false);
	}

	@Override
	public void dispose() {
		browser.dispose();
		super.dispose();
	}
	
}
