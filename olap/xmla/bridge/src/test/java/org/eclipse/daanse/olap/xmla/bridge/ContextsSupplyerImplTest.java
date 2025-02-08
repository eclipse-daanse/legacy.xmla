package org.eclipse.daanse.olap.xmla.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.daanse.olap.api.ContextGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class ContextsSupplyerImplTest {

	    @Mock
	    private ContextGroup contextGroup;
		
		@Test
		void get_Empty() throws Exception {
			when(contextGroup.getValidContexts()).thenReturn(List.of());
			ContextsSupplyerImpl csi= new ContextsSupplyerImpl(contextGroup);
			assertThat(csi.getContexts()).isNotNull().isEmpty();
		}
		
}
