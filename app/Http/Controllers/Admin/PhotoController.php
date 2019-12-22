<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Photo;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Session;

class PhotoController extends Controller
{
    /**
     * Display a listing of the resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function index()
    {
        $photos = Photo::paginate(10);
        return view('admin.photo.index',compact('photos'));
    }

    /**
     * Show the form for creating a new resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function create()
    {
        return view('admin.photo.create');
    }

   

    /**
     * Store a newly created resource in storage.
     *
     * @param Request $request
     * @return \Illuminate\Http\Response
     */
    
    public function store(Request $request)
    {
        $image = $request->file('file');
        $file_name = time().$image->getClientOriginalName();
        $image->move(public_path('/images'),$file_name);
        $photo = new Photo();
        $photo->original_name = $image->getClientOriginalName();
        $photo->path = '/images/'.$file_name;
        $photo->user_id = Auth::id();
        $photo->save();
        return response()->json(['success'=>$file_name,'photo_id'=>$photo->id]);
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
     * @param Request $request
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
        $photo = Photo::findOrFail($id);
        $file_name = $photo->original_name;
        $path = public_path().$photo->path;
        if(file_exists($path)){
            unlink($path);
            $photo->delete();
            Session::flash('delete', 'عکس مورد نظر با موفقیت حذف گردید.');
            return redirect('/admin/photos');
        }else{
            dd($path);
        }
    }
}
