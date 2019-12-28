<?php

namespace App\Http\Controllers\Admin;

use App\Charity;
use App\Category;
use App\Http\Controllers\Controller;
use App\Http\Requests\CharityRequest;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;

class CharityController extends Controller
{
    /**
     * Display a listing of the resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function index()
    {
        $charities = Charity::paginate(10);
        return view('admin.charity.index',compact('charities'));
    }

    /**
     * Show the form for creating a new resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function create()
    {
        $categories = Category::all();
        return view('admin.charity.create',compact('categories'));
    }

    /**
     * Store a newly created resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\Response
     */
    public function store(Request $request)
    {
        dd($request->all());
        $charity = new Charity();
        $charity->name = $request->input('name');
        $charity->owner = $request->input('owner');
        $charity->members = $request->input('members');
        $charity->description = $request->input('description');
        $charity->email = $request->input('email');
        $charity->address = $request->input('address');
        $charity->phone = $request->input('phone');
        $charity->account_number = $request->input('account_number');

        if ($request->input('slug')){
            $charity->slug = make_slug(($request->input('slug')));
        }
        else{
            $charity->slug = make_slug(($request->input('name')));
        }
        $charity->meta_title = $request->input('meta_title');
        $charity->meta_desc = $request->input('meta_desc');
        $charity->mata_key = $request->input('mata_key');
        $charity->category_id = $request->input('category');
        $charity->user_id = Auth::id();
        $charity->photo_id = $request->input('photo_id');
    }

    /**
     * Display the specified resource.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function show($id)
    {
        //
    }

    /**
     * Show the form for editing the specified resource.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function edit($id)
    {
        //
    }

    /**
     * Update the specified resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function update(Request $request, $id)
    {
        //
    }

    /**
     * Remove the specified resource from storage.
     *
     * @param  int  $id
     * @return \Illuminate\Http\Response
     */
    public function destroy($id)
    {
        //
    }
}
