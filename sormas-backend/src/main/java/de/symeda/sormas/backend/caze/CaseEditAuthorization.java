package de.symeda.sormas.backend.caze;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;


import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.backend.facility.Facility;
import de.symeda.sormas.backend.infrastructure.PointOfEntry;
import de.symeda.sormas.backend.region.Community;
import de.symeda.sormas.backend.region.District;
import de.symeda.sormas.backend.region.Region;
import de.symeda.sormas.backend.user.User;
import de.symeda.sormas.backend.user.UserService;

@Stateless (name = "CaseEditAuthorization")
@LocalBean
public class CaseEditAuthorization {
	
	@EJB
	private UserService userService;
	@EJB
	private CaseService caseService;

	public Boolean caseEditAllowedCheck(Case caze) {

		User user = userService.getCurrentUser();
       
        if (DataHelper.equal(user.getUuid(), (caze.getUuid()))) {
            return true;
        }

        if (userService.hasAnyRole(UserRole.getSupervisorRoles())) {
            return caze.getRegion().equals(user.getRegion());
        }

        if (userService.hasAnyRole(UserRole.getOfficerRoles())) {
            return caze.getDistrict().equals(user.getDistrict());
        }

        if ((userService.hasRole(UserRole.HOSPITAL_INFORMANT))) {
            return caze.getHealthFacility().equals(user.getHealthFacility());
        }

        if ((userService.hasRole(UserRole.COMMUNITY_INFORMANT))) {
            return caze.getCommunity().equals(user.getCommunity());
        }

        if ((userService.hasRole(UserRole.POE_INFORMANT))) {
            return caze.getPointOfEntry().equals(user.getPointOfEntry());
        }

        if (userService.hasRole(UserRole.NATIONAL_USER)) {
            return true;
        }
		
        return false;
    }
}