package com.example.a6laba;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostExtraction extends AppCompatActivity
{
    PostsAdapter postsAdapter;
    RecyclerView rcvAudio;
    private ArrayList<com.example.a6laba.Post> mListPosts = new ArrayList<com.example.a6laba.Post>();
    private final int resultAdd = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);
        // создаем адаптер
        postsAdapter = new PostsAdapter(getApplicationContext(), mListPosts, new PostsAdapter.RecycleItemClickListener()
        {
            //обработчик на нажатие поста из списка
            @Override
            public void onClickListener(com.example.a6laba.Post post, int position)
            {
                Toast.makeText(getApplicationContext(), post.title, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(PostExtraction.this, DetailPostActivity.class);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
        String maxPrice = "5000";
        String minPrice = "100";
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://r.onliner.by/sdapi/ak.api/search/apartments?price%5Bmin%5D=" + minPrice + "&price%5Bmax%5D=" + maxPrice + "&currency=usd&bounds%5Blb%5D%5Blat%5D=53.765346858917425&bounds%5Blb%5D%5Blong%5D=27.413028708853112&bounds%5Brt%5D%5Blat%5D=54.03091474781306&bounds%5Brt%5D%5Blong%5D=27.711908525658554&page=1&v=0.15562999284261325";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>()
                { //слушатель ответа от онлайнера
                    @Override
                    public void onResponse(String responseString)
                    {

                        try
                        {
                            JSONObject response = new JSONObject(responseString);
                            JSONArray jsonArray = response.getJSONArray("apartments");
                            for (int i = 0; i < 10; i++)
                            { // разбор 10 первых постов из onliner
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String photoUrl = jsonObject.getString("photo");
                                Bitmap image = null;
                                String title = jsonObject.getJSONObject("location").getString("address");
                                String link = jsonObject.getString("url");
                                Boolean owner = jsonObject.getJSONObject("contact").getBoolean("owner");
                                Double price = jsonObject.getJSONObject("price").getDouble("amount");
                                String currency = jsonObject.getJSONObject("price").getString("currency");
                                mListPosts.add(new com.example.a6laba.Post(image, photoUrl, null, title, link, owner, price, currency));
                            }
                            postsAdapter.setArray(mListPosts); // передаём посты без картинок в адаптер
                            Utility.savePostsInFile(getApplicationContext(), mListPosts); // сохраняем посты без картинок в файл
                            postsAdapter.notifyDataSetChanged(); //отрисовываем переданные посты пользователю
                            for (int i = 0; i < mListPosts.size(); i++)
                            {  // дозапрос картинок по ссылкам из постов
                                secondServiceCall(i, mListPosts.get(i).selectedImagePath);
                            }

                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                postsAdapter.setArray(mListPosts);
            }
        })
        {

            /**
             * Passing some request headers
             */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("accept", "application/json, text/plain, */*");
                return headers;
            }
        };
        // настраиваем RecyclerView
        //GridLayoutManager упорядочивает элементы в виде грида со столлбцами и строками
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 1, LinearLayoutManager.VERTICAL, false);
        rcvAudio = findViewById(R.id.rcv_posts);
        rcvAudio.setHasFixedSize(true);
        rcvAudio.setLayoutManager(gridLayoutManager);
        rcvAudio.setFocusable(false);
        // устанавливаем для списка адаптер
        rcvAudio.setAdapter(postsAdapter);

        try
        {
            mListPosts = Utility.getPostsList(getApplicationContext());
            if (mListPosts.size() == 0)
            {
                queue.add(stringRequest);
            } else
            {
                postsAdapter.setArray(mListPosts);
                for (int i = 0; i < mListPosts.size(); i++)
                {
                    secondServiceCall(i, mListPosts.get(i).selectedImagePath);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (JSONException e)
        {
            e.printStackTrace();
        }

    }

    public void secondServiceCall(int membershipid, String url)
    {
        // use this var membershipid acc to your need ...
        RequestQueue queue = Volley.newRequestQueue(this);

        ImageRequest ir = new ImageRequest(url, new Response.Listener<Bitmap>()
        {
            @Override
            public void onResponse(Bitmap response)
            {
                // callback
                com.example.a6laba.Post post = mListPosts.get(membershipid);//берём id поста из списка
                post.image = response; //полученный ответ сохраняем в пост

                postsAdapter.setArray(mListPosts);
                postsAdapter.notifyDataSetChanged();
            }
        }, 0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                postsAdapter.setArray(mListPosts);
            }
        });
        // 100 is your custom Size before Downloading the Image.
        queue.add(ir);
    }
}