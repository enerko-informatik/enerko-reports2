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
package de.enerko.reports2.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.enerko.reports2.AbstractDatabaseTest;

/**
 * @author Michael J. Simons, 2013-06-18
 */
public class FunctionBasedReportSourceTest extends AbstractDatabaseTest {
	@Test
	public void shouldHandlePipelinedFunction() throws SQLException {
		final String methodName = "pck_enerko_reports2_test.f_fb_report_source_test";
		
		final FunctionBasedReportSource reportSource = 
				new FunctionBasedReportSource(connection,  methodName, "5", "21.09.1979", "test");
		final List<CellDefinition> cellDefinitions = new ArrayList<CellDefinition>();
		for(CellDefinition cellDefinition : reportSource)
			cellDefinitions.add(cellDefinition);
		assertThat(cellDefinitions.size(), is(5));
	}
	
	@Test
	public void shouldHandlePipelinedFunctionWithComments() throws SQLException {
		final String methodName = "pck_enerko_reports2_test.f_fb_report_source_test2";
		
		final FunctionBasedReportSource reportSource = new FunctionBasedReportSource(connection,  methodName);
		final List<CellDefinition> cellDefinitions = new ArrayList<CellDefinition>();
		for(CellDefinition cellDefinition : reportSource)
			cellDefinitions.add(cellDefinition);
		assertThat(cellDefinitions.size(), is(2));
		
		CommentDefinition commentDefinition = cellDefinitions.get(0).comment; 
		assertThat(commentDefinition, notNullValue());
		assertThat(commentDefinition.author, is("HRE"));
		assertThat(commentDefinition.column, nullValue());
		assertThat(commentDefinition.row, nullValue());
		assertThat(commentDefinition.width, is(1));
		assertThat(commentDefinition.height, is(1));
		assertThat(commentDefinition.visible, is(false));
		
		commentDefinition = cellDefinitions.get(1).comment; 
		assertThat(commentDefinition, notNullValue());
		assertThat(commentDefinition.author, is("HRE"));
		assertThat(commentDefinition.column, is(23));
		assertThat(commentDefinition.row, is(42));
		assertThat(commentDefinition.width, is(3));
		assertThat(commentDefinition.height, is(4));
		assertThat(commentDefinition.visible, is(true));		
	}
}
