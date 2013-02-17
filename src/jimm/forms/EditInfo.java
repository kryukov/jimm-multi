/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-06  Jimm Project

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
File: src/jimm/JimmUI.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Igor Palkin
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" | protocols_JABBER is "true" | protocols_OBIMP is "true" #
package jimm.forms;

import jimm.ui.form.*;
import protocol.Protocol;
import jimm.search.*;

public class EditInfo implements FormListener {

    private static final int _NickNameItem = 1000;
    private static final int _FirstNameItem = 1001;
    private static final int _LastNameItem = 1002;
    private static final int _EmailItem = 1003;
    private static final int _BdayItem = 1004;
    private static final int _CellPhoneItem = 1005;
    private static final int _AddrItem = 1006;
    private static final int _CityItem = 1007;
    private static final int _StateItem = 1008;
    private static final int _SexItem = 1009;
    private static final int _HomePageItem = 1010;
    private static final int _WorkCompanyItem = 1011;
    private static final int _WorkDepartmentItem = 1012;
    private static final int _WorkPositionItem = 1013;
    private static final int _WorkPhoneItem = 1014;
    private static final int _AboutItem = 1015;
    private GraphForm form;
    private Protocol protocol;
    private UserInfo userInfo;

    public EditInfo(Protocol p, UserInfo info) {
        protocol = p;
        this.userInfo = info;
    }

    public EditInfo init() {
        // #sijapp cond.if protocols_JABBER is "true"#
        final boolean isJabber = (protocol instanceof protocol.jabber.Jabber);
        // #sijapp cond.end#
        // #sijapp cond.if protocols_OBIMP is "true"#
        final boolean isObimp = (protocol instanceof protocol.obimp.Obimp);
        // #sijapp cond.end#
        form = new GraphForm("editform", "save", "cancel", this);
        form.addTextField(_NickNameItem, "nick", userInfo.nick, 64);
        form.addTextField(_FirstNameItem, "firstname", userInfo.firstName, 64);
        form.addTextField(_LastNameItem, "lastname", userInfo.lastName, 64);
        // #sijapp cond.if protocols_JABBER is "true"#
        if (!isJabber) {
            // #sijapp cond.end#
            form.addSelector(_SexItem, "gender", "-" + "|" + "female" + "|" + "male", userInfo.gender);
            // #sijapp cond.if protocols_JABBER is "true"#
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_OBIMP is "true"#
        if (!isObimp) form.addTextField(_BdayItem, "birth_day", userInfo.birthDay, 15);
        // #sijapp cond.else#
        form.addTextField(_BdayItem, "birth_day", userInfo.birthDay, 15);
        // #sijapp cond.end#
        // #sijapp cond.if protocols_JABBER is "true"#
        if (isJabber) {
            form.addTextField(_EmailItem, "email", userInfo.email, 64);
            form.addTextField(_CellPhoneItem, "cell_phone", userInfo.cellPhone, 64);
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_OBIMP is "true"#
        if (isObimp) {
            form.addTextField(_EmailItem, "email", userInfo.email, 64);
            form.addTextField(_CellPhoneItem, "cell_phone", userInfo.cellPhone, 64);
        }
        // #sijapp cond.end#
        form.addTextField(_HomePageItem, "home_page", userInfo.homePage, 256);

        form.addHeader("home_info");
        // #sijapp cond.if protocols_JABBER is "true"#
        if (isJabber) {
            form.addTextField(_AddrItem, "addr", userInfo.homeAddress, 256);
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_OBIMP is "true"#
        if (isObimp) {
            form.addTextField(_AddrItem, "addr", userInfo.homeAddress, 256);
        }
        // #sijapp cond.end#
        form.addTextField(_CityItem, "city", userInfo.homeCity, 128);
        form.addTextField(_StateItem, "state", userInfo.homeState, 128);

        form.addHeader("work_info");
        form.addTextField(_WorkCompanyItem, "title", userInfo.workCompany, 256);
        form.addTextField(_WorkDepartmentItem, "depart", userInfo.workDepartment, 256);
        form.addTextField(_WorkPositionItem, "position", userInfo.workPosition, 256);
        // #sijapp cond.if protocols_JABBER is "true"#
        if (isJabber) {
            form.addTextField(_WorkPhoneItem, "phone", userInfo.workPhone, 64);
            form.addTextField(_AboutItem, "notes", userInfo.about, 2048);
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_OBIMP is "true"#
        if (isObimp) {
            form.addTextField(_WorkPhoneItem, "phone", userInfo.workPhone, 64);
        }
        // #sijapp cond.end#
        return this;
    }

    public void show() {
        form.show();
    }

    private void destroy() {
        form.destroy();
        protocol = null;
        form = null;
        userInfo = null;
    }

    public void formAction(GraphForm form, boolean apply) {
        if (!apply) {
            form.back();
            destroy();

        } else {
            boolean isJabber = false;
            // #sijapp cond.if protocols_JABBER is "true"#
            isJabber = (protocol instanceof protocol.jabber.Jabber);
            // #sijapp cond.end#
            // #sijapp cond.if protocols_OBIMP is "true"#
            final boolean isObimp = (protocol instanceof protocol.obimp.Obimp);
            // #sijapp cond.end#
            userInfo.nick = form.getTextFieldValue(_NickNameItem);
            // #sijapp cond.if protocols_OBIMP is "true"#
            if (!isObimp) userInfo.birthDay = form.getTextFieldValue(_BdayItem);
            // #sijapp cond.else#
            userInfo.birthDay = form.getTextFieldValue(_BdayItem);
            // #sijapp cond.end#
            // #sijapp cond.if protocols_JABBER is "true"#
            if (isJabber) {
                userInfo.email = form.getTextFieldValue(_EmailItem);
                userInfo.cellPhone = form.getTextFieldValue(_CellPhoneItem);
            }
            // #sijapp cond.end#
            // #sijapp cond.if protocols_OBIMP is "true"#
            if (isObimp) {
                userInfo.email = form.getTextFieldValue(_EmailItem);
                userInfo.cellPhone = form.getTextFieldValue(_CellPhoneItem);
            }
            // #sijapp cond.end#
            userInfo.firstName = form.getTextFieldValue(_FirstNameItem);
            userInfo.lastName = form.getTextFieldValue(_LastNameItem);
            if (!isJabber) {
                userInfo.gender = (byte) form.getSelectorValue(_SexItem);
            }
            userInfo.homePage = form.getTextFieldValue(_HomePageItem);

            // #sijapp cond.if protocols_JABBER is "true"#
            if (isJabber) {
                userInfo.homeAddress = form.getTextFieldValue(_AddrItem);
            }
            // #sijapp cond.end#
            // #sijapp cond.if protocols_OBIMP is "true"#
            if (isObimp) {
                userInfo.homeAddress = form.getTextFieldValue(_AddrItem);
            }
            // #sijapp cond.end#
            userInfo.homeCity = form.getTextFieldValue(_CityItem);
            userInfo.homeState = form.getTextFieldValue(_StateItem);

            userInfo.workCompany = form.getTextFieldValue(_WorkCompanyItem);
            userInfo.workDepartment = form.getTextFieldValue(_WorkDepartmentItem);
            userInfo.workPosition = form.getTextFieldValue(_WorkPositionItem);
            // #sijapp cond.if protocols_JABBER is "true"#
            if (isJabber) {
                userInfo.workPhone = form.getTextFieldValue(_WorkPhoneItem);
                userInfo.about = form.getTextFieldValue(_AboutItem);
            }
            // #sijapp cond.end#
            // #sijapp cond.if protocols_OBIMP is "true"#
            if (isObimp) {
                userInfo.workPhone = form.getTextFieldValue(_WorkPhoneItem);
            }
            // #sijapp cond.end#

            userInfo.updateProfileView();
            protocol.saveUserInfo(userInfo);
            form.back();
            destroy();
        }
    }
}
// #sijapp cond.end #

