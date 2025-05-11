package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class InternshipApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	private Item item1;
	private Item item2;

	@BeforeEach
	void setup() {
		item1 = new Item(1L, "Item1", "Description1", "NEW", "test1@example.com");
		item2 = new Item(2L, "Item2", "Description2", "NEW", "test2@example.com");
	}

	@Test
	void testGetAllItems() throws Exception {
		List<Item> items = Arrays.asList(item1, item2);
		when(itemService.findAll()).thenReturn(items);

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void testCreateItemSuccess() throws Exception {
		when(itemService.save(any(Item.class))).thenReturn(item1);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(item1)))
				.andExpect(status().isCreated());
	}

	@Test
	void testCreateItemInvalidEmail() throws Exception {
		item1.setEmail("invalid-email");

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(item1)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testGetItemByIdFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Item1")));
	}

	@Test
	void testGetItemByIdNotFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void testUpdateItem() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));
		when(itemService.save(any(Item.class))).thenReturn(item1);

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(item1)))
				.andExpect(status().isOk());
	}

	@Test
	void testDeleteItem() throws Exception {
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isNoContent());
	}

	@Test
	void testProcessItemsAsync() throws Exception {
		when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(List.of(item1)));

		MvcResult result = mockMvc.perform(get("/api/items/process"))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is("Item1")));
	}
}