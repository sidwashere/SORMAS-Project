/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.hospitalization;

import java.util.Arrays;
import java.util.Collection;

import com.vaadin.server.UserError;
import com.vaadin.ui.DateField;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.facility.FacilityReferenceDto;
import de.symeda.sormas.api.hospitalization.HospitalizationDto;
import de.symeda.sormas.api.hospitalization.PreviousHospitalizationDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.symptoms.SymptomsDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateComparisonValidator;
import de.symeda.sormas.ui.utils.FieldHelper;
import de.symeda.sormas.ui.utils.LayoutUtil;
import de.symeda.sormas.ui.utils.ViewMode;

@SuppressWarnings("serial")
public class HospitalizationForm extends AbstractEditForm<HospitalizationDto> {

	private static final String HEALTH_FACILITY = Captions.CaseHospitalization_healthFacility;	
	private final CaseDataDto caze;
	private final ViewMode viewMode;

	private static final String HTML_LAYOUT = 
			LayoutUtil.h3(I18nProperties.getString(Strings.headingHospitalization)) +
			LayoutUtil.fluidRowLocs(HEALTH_FACILITY, HospitalizationDto.ADMITTED_TO_HEALTH_FACILITY) +
			LayoutUtil.fluidRowLocs(HospitalizationDto.ADMISSION_DATE, HospitalizationDto.DISCHARGE_DATE, HospitalizationDto.LEFT_AGAINST_ADVICE, "") +
			LayoutUtil.fluidRowLocs(HospitalizationDto.ACCOMMODATION, HospitalizationDto.ISOLATED, HospitalizationDto.ISOLATION_DATE, "") +
			LayoutUtil.fluidRow(LayoutUtil.fluidColumnLocCss(CssStyles.VSPACE_TOP_3, 6, 0, HospitalizationDto.HOSPITALIZED_PREVIOUSLY)) +
			LayoutUtil.fluidRowLocs(HospitalizationDto.PREVIOUS_HOSPITALIZATIONS)
			;		

	public HospitalizationForm(CaseDataDto caze, UserRight editOrCreateUserRight, ViewMode viewMode) {
		super(HospitalizationDto.class, HospitalizationDto.I18N_PREFIX, editOrCreateUserRight);
		this.caze = caze;
		this.viewMode = viewMode;
		addFields();
	}

	@Override
	protected void addFields() {
		if (caze == null || viewMode == null) {
			return;
		}

		TextField facilityField = addCustomField(HEALTH_FACILITY, FacilityReferenceDto.class, TextField.class);
		facilityField.setValue(caze.getHealthFacility().toString());
		facilityField.setReadOnly(true);

		addField(HospitalizationDto.ADMITTED_TO_HEALTH_FACILITY, OptionGroup.class);
		DateField admissionDateField = addField(HospitalizationDto.ADMISSION_DATE, DateField.class);
		DateField dischargeDateField = addDateField(HospitalizationDto.DISCHARGE_DATE, DateField.class, 7);
		addFields(HospitalizationDto.ACCOMMODATION, HospitalizationDto.ISOLATION_DATE);
		addField(HospitalizationDto.ISOLATED, OptionGroup.class);
		addField(HospitalizationDto.LEFT_AGAINST_ADVICE, OptionGroup.class);
		OptionGroup hospitalizedPreviouslyField = addField(HospitalizationDto.HOSPITALIZED_PREVIOUSLY, OptionGroup.class);
		CssStyles.style(hospitalizedPreviouslyField, CssStyles.ERROR_COLOR_PRIMARY);
		PreviousHospitalizationsField previousHospitalizationsField = addField(HospitalizationDto.PREVIOUS_HOSPITALIZATIONS, PreviousHospitalizationsField.class);

		initializeVisibilitiesAndAllowedVisibilities(null, viewMode);

		if (isVisibleAllowed(HospitalizationDto.ISOLATION_DATE)) {
			FieldHelper.setVisibleWhen(getFieldGroup(), HospitalizationDto.ISOLATION_DATE, HospitalizationDto.ISOLATED, Arrays.asList(YesNoUnknown.YES), true);
		}
		if (isVisibleAllowed(HospitalizationDto.LEFT_AGAINST_ADVICE)) {
			FieldHelper.setVisibleWhenNotNull(getFieldGroup(), HospitalizationDto.LEFT_AGAINST_ADVICE, HospitalizationDto.DISCHARGE_DATE, true);
		}
		if (isVisibleAllowed(HospitalizationDto.PREVIOUS_HOSPITALIZATIONS)) {
			FieldHelper.setVisibleWhen(getFieldGroup(), HospitalizationDto.PREVIOUS_HOSPITALIZATIONS, HospitalizationDto.HOSPITALIZED_PREVIOUSLY, Arrays.asList(YesNoUnknown.YES), true);
		}

		// Validations
		admissionDateField.addValidator(new DateComparisonValidator(admissionDateField, caze.getSymptoms().getOnsetDate(), false, false, 
				I18nProperties.getValidationError(Validations.afterDateSoft, admissionDateField.getCaption(), I18nProperties.getPrefixCaption(SymptomsDto.I18N_PREFIX, SymptomsDto.ONSET_DATE))));
		admissionDateField.addValidator(new DateComparisonValidator(admissionDateField, dischargeDateField, true, false, 
				I18nProperties.getValidationError(Validations.beforeDate, admissionDateField.getCaption(), dischargeDateField.getCaption())));
		admissionDateField.setInvalidCommitted(true);
		dischargeDateField.addValidator(new DateComparisonValidator(dischargeDateField, admissionDateField, false, false, 
				I18nProperties.getValidationError(Validations.afterDate, dischargeDateField.getCaption(), admissionDateField.getCaption())));

		hospitalizedPreviouslyField.addValueChangeListener(e -> {
			updatePrevHospHint(hospitalizedPreviouslyField, previousHospitalizationsField);
		});
		previousHospitalizationsField.addValueChangeListener(e -> {
			updatePrevHospHint(hospitalizedPreviouslyField, previousHospitalizationsField);
		});
	}

	private void updatePrevHospHint(OptionGroup hospitalizedPreviouslyField, PreviousHospitalizationsField previousHospitalizationsField) {
		YesNoUnknown value = (YesNoUnknown) hospitalizedPreviouslyField.getValue();
		Collection<PreviousHospitalizationDto> previousHospitalizations = previousHospitalizationsField.getValue();
		if (UserProvider.getCurrent().hasUserRight(UserRight.CASE_EDIT) && value == YesNoUnknown.YES && (previousHospitalizations == null || previousHospitalizations.size() == 0)) {
			hospitalizedPreviouslyField.setComponentError(new UserError(I18nProperties.getValidationError(Validations.softAddEntryToList)));
		} else {
			hospitalizedPreviouslyField.setComponentError(null);
		}
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}

}