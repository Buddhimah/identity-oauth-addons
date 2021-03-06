
/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License
 */

package org.wso2.carbon.identity.oauth2.token.handler.clientauth.jwt.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.client.authentication.OAuthClientAuthnException;
import org.wso2.carbon.identity.oauth2.token.handler.clientauth.jwt.Constants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * JWT token persistence is managed by JWTStorageManager
 * It saved JWTEntry instances in Identity Database.
 */
public class JWTStorageManager {

    private static final Log log = LogFactory.getLog(JWTStorageManager.class);

    /**
     * Check whether a JWT Entry with given jti exists in the DB.
     *
     * @param jti JWT token id
     * @return true if an entry is found
     * @throws IdentityOAuth2Exception when exception occurs
     */
    public boolean isJTIExistsInDB(String jti) throws OAuthClientAuthnException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        boolean isExists = false;
        ResultSet rs = null;
        try {
            prepStmt = dbConnection.prepareStatement(Constants.SQLQueries.GET_JWT_ID);
            prepStmt.setString(1, jti);
            rs = prepStmt.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            if (count > 0) {
                isExists = true;
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error when retrieving the JWT ID: " + jti, e);
            }
            throw new OAuthClientAuthnException("Error occurred while validating the JTI: " + jti + " of the " +
                                                "assertion.", OAuth2ErrorCodes.INVALID_REQUEST);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }
        return isExists;
    }

    /**
     * To get persisted JWT for a given JTI.
     *
     * @param jti jti
     * @return JWTEntry
     * @throws IdentityOAuth2Exception
     */
    public JWTEntry getJwtFromDB(String jti) throws OAuthClientAuthnException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        JWTEntry jwtEntry = null;
        try {
            prepStmt = dbConnection.prepareStatement(Constants.SQLQueries.GET_JWT);
            prepStmt.setString(1, jti);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                long exp = rs.getTime(1, Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC))).getTime();
                long created = rs.getTime(2, Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC))).getTime();
                jwtEntry = new JWTEntry(exp, created);
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error when retrieving the JWT ID: " + jti, e);
            }
            throw new OAuthClientAuthnException("Error occurred while validating the JTI: " + jti + " of the " +
                                                "assertion.", OAuth2ErrorCodes.INVALID_REQUEST);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }
        return jwtEntry;
    }

    /**
     * To persist unique id for jti in the table.
     *
     * @param jti         jti a unique id
     * @param expTime     expiration time
     * @param timeCreated jti inserted time
     * @throws IdentityOAuth2Exception
     */
    public void persistJWTIdInDB(String jti, long expTime, long timeCreated) throws OAuthClientAuthnException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            preparedStatement = connection.prepareStatement(Constants.SQLQueries.INSERT_JWD_ID);
            preparedStatement.setString(1, jti);
            Timestamp timestamp = new Timestamp(timeCreated);
            Timestamp expTimestamp = new Timestamp(expTime);
            preparedStatement.setTimestamp(2, expTimestamp, Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC)));
            preparedStatement.setTimestamp(3, timestamp,
                    Calendar.getInstance(TimeZone.getTimeZone(Constants.UTC)));
            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.commit();
        } catch (SQLException e) {
            String error = "Error when storing the JWT ID: " + jti + " with exp: " + expTime;
            if (log.isDebugEnabled()) {
                log.debug(error, e);
            }
            throw new OAuthClientAuthnException("Error occurred while validating the JTI: " + jti + " of the " +
                                                "assertion.", OAuth2ErrorCodes.INVALID_REQUEST);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }
}
