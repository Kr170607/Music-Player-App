package com.example.musicplayer

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.Serializable
import java.util.Locale
import android.transition.TransitionInflater

class Home_frag : Fragment() {

    private var myRecyclerView: RecyclerView? = null
    private var myAdapter: MyAdapter? = null
    private var songList: List<Data> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_frag, container, false)
        
        myRecyclerView = view.findViewById(R.id.recyclerView)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        val retrofitBuilder = Retrofit.Builder()
            .baseUrl("https://discoveryprovider.audius.co/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api_interface::class.java)

        val retrofitData = retrofitBuilder.getProductData()
        
        retrofitData.enqueue(object : Callback<AudiusResponse?> {
            override fun onResponse(call: Call<AudiusResponse?>, response: Response<AudiusResponse?>) {
                if (response.isSuccessful && isAdded) {
                    val responseBody = response.body()
                    val dataList = responseBody?.data
                    if (dataList != null) {
                        songList = dataList
                        myAdapter = MyAdapter(requireActivity(), songList) { song, imageView ->
                            val currentIndex = songList.indexOf(song)
                            val playerFrag = Player_frag()
                            val bundle = Bundle()
                            bundle.putSerializable("songList", songList as Serializable)
                            bundle.putInt("currentIndex", currentIndex)
                            playerFrag.arguments = bundle
                            
                            // Start Shared Element Transition
                            val fragmentManager = parentFragmentManager
                            fragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .addSharedElement(imageView, "song_image_transition")
                                .replace(R.id.FrameLayout, playerFrag)
                                .addToBackStack(null)
                                .commit()
                            
                            // Update bottom navigation state if needed
                            (activity as? WelcomePg)?.updateBottomNavForPlayer()
                        }
                        myRecyclerView?.adapter = myAdapter
                        myRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
                    }
                }
            }

            override fun onFailure(call: Call<AudiusResponse?>, t: Throwable) {
                Log.d("Home_frag", "onFailure: " + t.message)
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })

        return view
    }

    private fun filterList(query: String?) {
        val adapter = myAdapter ?: return
        if (query != null) {
            val filteredList = ArrayList<Data>()
            for (song in songList) {
                if (song.title?.lowercase(Locale.ROOT)?.contains(query.lowercase(Locale.ROOT)) == true) {
                    filteredList.add(song)
                }
            }
            adapter.setFilteredList(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        myRecyclerView = null
        myAdapter = null
    }
}
