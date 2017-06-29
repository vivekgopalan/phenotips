/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import org.phenotips.data.permissions.AccessLevel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link OwnerAccessAuthorizationModule owner granted access} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class OwnerAccessAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<AuthorizationModule>(OwnerAccessAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Right right;

    @Mock
    private Patient patient;

    @Mock
    private PatientAccess patientAccess;

    @Mock
    private AccessLevel accessLevel;

    private DocumentReference doc = new DocumentReference("xwiki", "data", "P01");

    @Mock
    private DocumentReference userProfile;

    @Before
    public void setupMocks()
    {
        MockitoAnnotations.initMocks(this);
        when(this.user.getProfileDocument()).thenReturn(this.userProfile);
    }

    @Test
    public void accessGrantedWithOwner() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.get("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        PatientAccessHelper helper = this.mocker.getInstance(PatientAccessHelper.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.patientAccess);
        when(helper.getAccessLevel(this.patient, this.userProfile)).thenReturn(this.accessLevel);
        when(this.patientAccess.isOwner(this.userProfile)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void accessWithGroupTest() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.get("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        PatientAccessHelper helper = this.mocker.getInstance(PatientAccessHelper.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.patientAccess);
        when(helper.getAccessLevel(this.patient, this.userProfile)).thenReturn(this.accessLevel);
        when(this.patientAccess.isOwner(this.userProfile)).thenReturn(false);
        when(this.accessLevel.getName()).thenReturn("owner");
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void accessGrantedWithOwnerAndNullRight() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.get("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.patientAccess);
        PatientAccessHelper helper = this.mocker.getInstance(PatientAccessHelper.class);
        when(helper.getAccessLevel(this.patient, this.userProfile)).thenReturn(this.accessLevel);
        when(this.patientAccess.isOwner(this.userProfile)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, null, this.doc));
    }

    @Test
    public void noActionWithNonOwner() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.get("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.patientAccess);
        PatientAccessHelper helper = this.mocker.getInstance(PatientAccessHelper.class);
        when(helper.getAccessLevel(this.patient, this.userProfile)).thenReturn(this.accessLevel);
        when(this.patientAccess.isOwner(this.userProfile)).thenReturn(false);
        when(this.accessLevel.getName()).thenReturn("none");
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void noActionWithNonPatient() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void noActionWithNullArguments() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, this.right, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, null));
    }

    @Test
    public void expectedPriority() throws ComponentLookupException
    {
        Assert.assertEquals(400, this.mocker.getComponentUnderTest().getPriority());
    }
}