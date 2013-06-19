/*
 * Copyright 2013 ENERKO Informatik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * THIS SOFTWARE IS  PROVIDED BY THE  COPYRIGHT HOLDERS AND  CONTRIBUTORS "AS IS"
 * AND ANY  EXPRESS OR  IMPLIED WARRANTIES,  INCLUDING, BUT  NOT LIMITED  TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL  THE COPYRIGHT HOLDER OR CONTRIBUTORS  BE LIABLE
 * FOR ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL,  EXEMPLARY, OR  CONSEQUENTIAL
 * DAMAGES (INCLUDING,  BUT NOT  LIMITED TO,  PROCUREMENT OF  SUBSTITUTE GOODS OR
 * SERVICES; LOSS  OF USE,  DATA, OR  PROFITS; OR  BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT  LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE  USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.enerko.reports2;

import static de.enerko.reports2.Unchecker.uncheck;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.DriverManager;
import java.sql.SQLException;

import oracle.jdbc.OracleConnection;
import oracle.sql.BLOB;

/**
 * This is the main entry point for the PL/SQL package pck_enerko_reports2
 * @author Michael J. Simons, 2013-06-19
 */
public class PckEnerkoReports2 {
	final static OracleConnection connection;
	final static ReportEngine reportEngine;
	static {
		try {
			// Open the default, internal JDBC connection
			connection = (OracleConnection) DriverManager.getConnection("jdbc:default:connection:");
			reportEngine = new ReportEngine(connection);		
		} catch (SQLException e) {
			throw uncheck(e);
		}		
	}
	
	public static BLOB createReportFromStatement(final String statement) throws SQLException, IOException {
		
		final Report report = reportEngine.createReportFromStatement(statement);
		
		final BLOB rv = BLOB.createTemporary(connection, true, BLOB.DURATION_SESSION);
		final OutputStream out = new BufferedOutputStream(rv.setBinaryStream(0));
		report.write(out);
		
		return rv;
	}
}